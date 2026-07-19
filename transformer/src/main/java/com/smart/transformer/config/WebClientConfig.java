package com.smart.transformer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String supabaseServiceRoleKey;

    @Value("${ai-sidecar.base-url}")
    private String aiSidecarBaseUrl;

    /**
     * Talks to Supabase Storage's REST API using the service role key
     * (bypasses RLS — never expose this key to the frontend).
     */
    @Bean
    public WebClient supabaseWebClient() {
        return WebClient.builder()
                .baseUrl(supabaseUrl)
                .defaultHeader("Authorization", "Bearer " + supabaseServiceRoleKey)
                .defaultHeader("apikey", supabaseServiceRoleKey)
                .build();
    }

    /**
     * Talks to the FastAPI sidecar for Isolation Forest scoring / LLM chat.
     */
    @Bean
    public WebClient aiSidecarWebClient() {
        return WebClient.builder()
                .baseUrl(aiSidecarBaseUrl)
                .build();
    }
}
