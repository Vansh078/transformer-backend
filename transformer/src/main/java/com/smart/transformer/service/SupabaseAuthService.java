package com.smart.transformer.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smart.transformer.exception.AuthenticationFailedException;
import com.smart.transformer.exception.DuplicateResourceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Thin client for the Supabase Auth (GoTrue) REST API. The backend never issues or signs
 * JWTs itself — Supabase Auth owns credential storage, password hashing, session issuance,
 * and OAuth provider flows. This class only talks to the handful of endpoints the app needs
 * for email/password registration & login and for verifying a Google-issued access token.
 */
@Slf4j
@Service
public class SupabaseAuthService {

    /** Upper bound on any single Supabase Auth call so a slow/hanging response can't pin a
     *  servlet thread indefinitely — this sits on the login/register hot path. */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebClient supabaseWebClient;

    public SupabaseAuthService(WebClient supabaseWebClient) {
        this.supabaseWebClient = supabaseWebClient;
    }

    /**
     * Creates a new Supabase Auth user with an email/password credential.
     * <p>Depending on the project's "Confirm email" setting, Supabase returns either a bare
     * user object ({@code {id, email, ...}}) when confirmation is required, or a full session
     * (user nested under {@code user}) when it auto-confirms. Both shapes are handled here.
     */
    @SuppressWarnings("unchecked")
    public SupabaseUser signUp(String email, String password) {
        try {
            Map<String, Object> response = supabaseWebClient.post()
                    .uri("/auth/v1/signup")
                    .bodyValue(Map.of("email", email, "password", password))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();

            if (response == null) {
                throw new AuthenticationFailedException("Registration failed. Please try again.");
            }
            Map<String, Object> userNode = response.containsKey("user")
                    ? (Map<String, Object>) response.get("user")
                    : response;
            Object id = userNode != null ? userNode.get("id") : null;
            if (id == null) {
                throw new AuthenticationFailedException("Registration failed. Please try again.");
            }
            Object userEmail = userNode.get("email");
            return new SupabaseUser(UUID.fromString(id.toString()),
                    userEmail != null ? userEmail.toString() : email, null);
        } catch (WebClientResponseException e) {
            String msg = extractMessage(e);
            log.warn("Supabase signUp failed for {}: {}", email, msg);
            if (e.getStatusCode().value() == 422 || (msg != null && msg.toLowerCase().contains("already"))) {
                throw new DuplicateResourceException("A user with email '" + email + "' already exists");
            }
            // Only forward Supabase's message when it's a known user-actionable validation
            // complaint (e.g. weak password); otherwise avoid echoing provider-internal text
            // back to the client, matching the normalized-message policy used for login.
            if (msg != null && msg.toLowerCase().contains("password")) {
                throw new AuthenticationFailedException(msg);
            }
            throw new AuthenticationFailedException("Registration failed. Please try again.");
        }
    }

    /** Verifies email/password credentials and returns the issued session (access + refresh tokens). */
    public SupabaseSession signInWithPassword(String email, String password) {
        try {
            SessionResponse response = supabaseWebClient.post()
                    .uri("/auth/v1/token?grant_type=password")
                    .bodyValue(Map.of("email", email, "password", password))
                    .retrieve()
                    .bodyToMono(SessionResponse.class)
                    .timeout(TIMEOUT)
                    .block();

            if (response == null || response.accessToken() == null) {
                throw new AuthenticationFailedException("Invalid email or password");
            }
            return toSession(response);
        } catch (WebClientResponseException e) {
            log.warn("Supabase login failed for {}: {}", email, extractMessage(e));
            throw new AuthenticationFailedException("Invalid email or password");
        }
    }

    /**
     * Verifies a Supabase-issued access token (as obtained from the client-side Google OAuth
     * flow) and returns the identity Supabase resolves for it. Throws if the token is invalid,
     * expired, or revoked.
     */
    @SuppressWarnings("unchecked")
    public SupabaseUser getUserFromAccessToken(String accessToken) {
        try {
            Map<String, Object> response = supabaseWebClient.get()
                    .uri("/auth/v1/user")
                    .headers(h -> h.set("Authorization", "Bearer " + accessToken))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();

            Object id = response != null ? response.get("id") : null;
            if (id == null) {
                throw new AuthenticationFailedException("Invalid or expired access token");
            }
            Object email = response.get("email");
            return new SupabaseUser(UUID.fromString(id.toString()),
                    email != null ? email.toString() : null, extractDisplayName(response));
        } catch (WebClientResponseException e) {
            log.warn("Supabase token verification failed: {}", extractMessage(e));
            throw new AuthenticationFailedException("Invalid or expired access token");
        }
    }

    /**
     * Deletes a Supabase Auth user via the admin API. Used to unwind a signup when the local
     * {@code app_users} half of registration fails, so we don't strand a credential-only
     * account that can never be paired with a local profile.
     */
    public void deleteUser(UUID supabaseUserId) {
        try {
            supabaseWebClient.delete()
                    .uri("/auth/v1/admin/users/{id}", supabaseUserId)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(TIMEOUT)
                    .block();
        } catch (Exception e) {
            log.error("Failed to roll back orphaned Supabase Auth user {}: {}", supabaseUserId, e.getMessage());
        }
    }

    private SupabaseSession toSession(SessionResponse r) {
        String displayName = r.user() != null && r.user().userMetadata() != null
                ? displayNameFrom(r.user().userMetadata())
                : null;
        return new SupabaseSession(
                r.accessToken(), r.refreshToken(), r.tokenType(), r.expiresIn(),
                new SupabaseUser(UUID.fromString(r.user().id()), r.user().email(), displayName));
    }

    @SuppressWarnings("unchecked")
    private String extractDisplayName(Map<String, Object> userNode) {
        Object metadata = userNode.get("user_metadata");
        return metadata instanceof Map<?, ?> userMetadata ? displayNameFrom((Map<String, Object>) userMetadata) : null;
    }

    private String displayNameFrom(Map<String, Object> userMetadata) {
        Object fullName = userMetadata.get("full_name");
        if (fullName == null) fullName = userMetadata.get("name");
        return fullName != null ? fullName.toString() : null;
    }

    private String extractMessage(WebClientResponseException e) {
        try {
            Map<?, ?> body = e.getResponseBodyAs(Map.class);
            if (body != null) {
                Object msg = body.get("msg");
                if (msg == null) msg = body.get("error_description");
                if (msg == null) msg = body.get("message");
                if (msg == null) msg = body.get("error");
                if (msg != null) return msg.toString();
            }
        } catch (Exception ignored) {
            // fall through to raw body below
        }
        return e.getResponseBodyAsString();
    }

    // --- minimal shapes for the Supabase Auth REST responses we consume ---

    /** @param displayName resolved from {@code user_metadata.full_name}/{@code .name} when available (e.g. Google profile name); null for plain email/password accounts. */
    public record SupabaseUser(UUID id, String email, String displayName) {}

    public record SupabaseSession(String accessToken, String refreshToken, String tokenType,
                                   long expiresIn, SupabaseUser user) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SessionUser(String id, String email,
                                @JsonProperty("user_metadata") Map<String, Object> userMetadata) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SessionResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn,
            SessionUser user) {
    }
}
