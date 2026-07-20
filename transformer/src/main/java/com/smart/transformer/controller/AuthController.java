package com.smart.transformer.controller;

import com.smart.transformer.dto.request.GoogleAuthRequest;
import com.smart.transformer.dto.request.LoginRequest;
import com.smart.transformer.dto.request.RegisterRequest;
import com.smart.transformer.dto.response.ApiResponse;
import com.smart.transformer.dto.response.AuthResponse;
import com.smart.transformer.dto.response.UserResponse;
import com.smart.transformer.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication module. Supabase Auth owns credential storage and token issuance;
 * this controller synchronizes the local {@code app_users} profile around it.
 * Per {@code SecurityConfig}, only {@code /register}, {@code /login}, and {@code /google} are
 * reachable without a JWT — {@code /me} and {@code /logout} require one, since they act on the
 * already-authenticated caller.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Creates a Supabase Auth credential + local profile for email/password sign-up. */
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("Registration successful", authService.register(request));
    }

    /** Authenticates email/password credentials and returns Supabase-issued tokens. */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Login successful", authService.login(request));
    }

    /**
     * Synchronizes a Google-authenticated user (Supabase OAuth) with the local database.
     * Handles both first-time registration and subsequent logins.
     */
    @PostMapping("/google")
    public ApiResponse<UserResponse> google(@Valid @RequestBody GoogleAuthRequest request) {
        return ApiResponse.success("Google authentication successful", authService.authenticateWithGoogle(request));
    }

    /** Returns the application profile for the currently authenticated user. */
    @GetMapping("/me")
    public ApiResponse<UserResponse> currentUser() {
        return ApiResponse.success(authService.getCurrentUser());
    }

    /** Client-side session termination; records the sign-out for audit purposes. */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success("Logged out", null);
    }
}
