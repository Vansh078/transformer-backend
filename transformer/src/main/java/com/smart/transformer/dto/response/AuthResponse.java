package com.smart.transformer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Returned after successful registration/login. Tokens are issued by Supabase Auth —
 * this backend never generates or signs JWTs itself.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserResponse user;
}
