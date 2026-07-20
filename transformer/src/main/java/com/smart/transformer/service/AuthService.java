package com.smart.transformer.service;

import com.smart.transformer.dto.request.GoogleAuthRequest;
import com.smart.transformer.dto.request.LoginRequest;
import com.smart.transformer.dto.request.RegisterRequest;
import com.smart.transformer.dto.response.AuthResponse;
import com.smart.transformer.dto.response.UserResponse;
import com.smart.transformer.entity.Role;
import com.smart.transformer.entity.User;
import com.smart.transformer.entity.enums.AuthProvider;
import com.smart.transformer.entity.enums.RoleName;
import com.smart.transformer.exception.AuthenticationFailedException;
import com.smart.transformer.exception.DuplicateResourceException;
import com.smart.transformer.exception.ResourceNotFoundException;
import com.smart.transformer.repository.RoleRepository;
import com.smart.transformer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the authentication module: Supabase Auth owns credentials and JWT issuance,
 * this service keeps the local {@code app_users} profile (name, role, organization, provider)
 * in sync with it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Role assigned automatically on first sign-in (registration or first Google login). */
    private static final RoleName DEFAULT_ROLE = RoleName.VIEWER;

    private final SupabaseAuthService supabaseAuthService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ActivityLogService activityLogService;

    /**
     * Registers a new email/password user: creates the Supabase Auth credential, then
     * provisions the local profile with the default role.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("A user with email '" + request.getEmail() + "' already exists");
        }

        SupabaseAuthService.SupabaseUser supabaseUser = supabaseAuthService.signUp(request.getEmail(), request.getPassword());

        try {
            // Race guard: another request may have provisioned this id/email between the check
            // above and Supabase's response.
            if (userRepository.existsById(supabaseUser.id()) || userRepository.existsByEmail(supabaseUser.email())) {
                throw new DuplicateResourceException("A user with email '" + request.getEmail() + "' already exists");
            }

            User user = new User();
            user.setId(supabaseUser.id());
            user.setEmail(supabaseUser.email());
            user.setFullName(request.getFullName());
            user.setOrganization(request.getOrganization());
            user.setAuthProvider(AuthProvider.EMAIL);
            user.setRole(defaultRole());
            user.setActive(true);

            UserResponse response = toResponse(userRepository.save(user));
            activityLogService.record("USER_REGISTERED", "User", user.getId(), "Email: " + user.getEmail());
            return response;
        } catch (RuntimeException e) {
            // The Supabase Auth credential was already created and can't be rolled back by our
            // local @Transactional boundary. Unwind it here so a local-side failure doesn't leave
            // a Supabase account with no path to ever getting a local profile (register would see
            // it as a duplicate; login would find no app_users row).
            supabaseAuthService.deleteUser(supabaseUser.id());
            throw e;
        }
    }

    /** Authenticates email/password credentials against Supabase and returns the issued session. */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        SupabaseAuthService.SupabaseSession session = supabaseAuthService.signInWithPassword(
                request.getEmail(), request.getPassword());

        User user = userRepository.findById(session.user().id())
                .orElseThrow(() -> new AuthenticationFailedException(
                        "No application profile found for this account. Please contact an administrator."));

        if (!user.isActive()) {
            throw new AuthenticationFailedException("This account has been deactivated");
        }

        activityLogService.record("USER_LOGIN", "User", user.getId(), "Provider: EMAIL");
        return new AuthResponse(session.accessToken(), session.refreshToken(), session.tokenType(),
                session.expiresIn(), toResponse(user));
    }

    /**
     * Synchronizes a Google-authenticated user with the local database. The client obtains the
     * Supabase access token via the Google OAuth redirect flow (supabase-js `signInWithOAuth`)
     * before calling this; here we verify that token with Supabase and either create the profile
     * (first login) or fetch the existing one (subsequent logins).
     */
    @Transactional
    public UserResponse authenticateWithGoogle(GoogleAuthRequest request) {
        SupabaseAuthService.SupabaseUser supabaseUser = supabaseAuthService.getUserFromAccessToken(request.getAccessToken());

        Optional<User> byId = userRepository.findById(supabaseUser.id());
        if (byId.isPresent()) {
            User existing = byId.get();
            activityLogService.record("USER_LOGIN", "User", existing.getId(), "Provider: GOOGLE");
            return toResponse(existing);
        }

        // First time we've seen this Supabase user id. If the email is already registered under
        // a different id (e.g. an existing EMAIL-provider account), fail with a clear, actionable
        // error instead of hitting app_users' unique email constraint and surfacing a raw 500.
        if (supabaseUser.email() != null && userRepository.existsByEmail(supabaseUser.email())) {
            throw new DuplicateResourceException(
                    "An account with email '" + supabaseUser.email() + "' already exists under a different sign-in method");
        }

        User user = new User();
        user.setId(supabaseUser.id());
        user.setEmail(supabaseUser.email());
        user.setFullName(supabaseUser.displayName() != null ? supabaseUser.displayName() : supabaseUser.email());
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setRole(defaultRole());
        user.setActive(true);
        UserResponse response = toResponse(userRepository.save(user));
        activityLogService.record("USER_REGISTERED", "User", user.getId(), "Provider: GOOGLE");
        return response;
    }

    /** Returns the application profile for the currently authenticated (JWT) principal. */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        UUID id = currentUserId()
                .orElseThrow(() -> new AuthenticationFailedException("No authenticated user in context"));
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return toResponse(user);
    }

    /**
     * Client-side session termination. Supabase sessions are stateless JWTs validated by the
     * resource server on every request, so there is nothing for the backend to invalidate here;
     * this simply records the sign-out for audit purposes. Clients should discard their tokens
     * and, if desired, call Supabase's own {@code /auth/v1/logout} to revoke the refresh token.
     */
    @Transactional
    public void logout() {
        currentUserId().ifPresent(id -> activityLogService.record("USER_LOGOUT", "User", id, null));
    }

    private Role defaultRole() {
        return roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + DEFAULT_ROLE));
    }

    private Optional<UUID> currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            try {
                return Optional.of(UUID.fromString(jwt.getSubject()));
            } catch (IllegalArgumentException e) {
                log.warn("JWT subject is not a valid UUID: {}", jwt.getSubject());
            }
        }
        return Optional.empty();
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(), u.getEmail(), u.getFullName(),
                u.getRole() != null ? u.getRole().getName() : null,
                u.isActive(), u.getCreatedAt(),
                u.getAuthProvider(), u.getOrganization());
    }
}
