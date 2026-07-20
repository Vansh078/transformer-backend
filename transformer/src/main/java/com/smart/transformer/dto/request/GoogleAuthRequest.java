package com.smart.transformer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * The actual Google OAuth redirect/consent flow is handled by Supabase on the client
 * (supabase-js `signInWithOAuth`). Once that completes, the client already holds a valid
 * Supabase-issued access token for the Google-authenticated user. This request carries that
 * token so the backend can verify it and synchronize (create-if-first-login / fetch) the
 * matching `app_users` profile.
 */
@Getter
@Setter
public class GoogleAuthRequest {

    @NotBlank(message = "Supabase access token is required")
    private String accessToken;
}
