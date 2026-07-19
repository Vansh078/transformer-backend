package com.smart.transformer.exception;

/** Thrown when Supabase rejects credentials, a token is missing/invalid, or an account is deactivated. */
public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
