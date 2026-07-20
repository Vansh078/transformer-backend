package com.smart.transformer.entity.enums;

/**
 * How the user authenticates with Supabase Auth.
 * EMAIL  — traditional email/password credentials managed by Supabase Auth.
 * GOOGLE — Google Sign-In via Supabase OAuth.
 */
public enum AuthProvider {
    EMAIL,
    GOOGLE
}
