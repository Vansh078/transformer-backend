package com.smart.transformer.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Thin wrapper around OpenAI's Chat Completions API. Centralizes the request/response
 * shapes and error handling so every AI feature (chat, explainability, incident
 * narratives, maintenance summaries) goes through one place.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final WebClient openAiWebClient;

    @Value("${openai.model:gpt-4o-mini}")
    private String defaultModel;

    /** Convenience overload: single system prompt + single user prompt, default temperature. */
    public String complete(String systemPrompt, String userPrompt) {
        return complete(List.of(
                new Message("system", systemPrompt),
                new Message("user", userPrompt)
        ), 0.3);
    }

    /** Full control over the message list (used for multi-turn chat) and temperature. */
    public String complete(List<Message> messages, double temperature) {
        CompletionRequest body = new CompletionRequest(defaultModel, messages, temperature);

        try {
            CompletionResponse response = openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(CompletionResponse.class)
                    .block();

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                return "I couldn't generate a response right now — please try again.";
            }

            return response.choices().get(0).message().content();
        } catch (Exception e) {
            log.error("OpenAI call failed: {}", e.getMessage());
            return "The AI assistant is temporarily unavailable — please try again shortly.";
        }
    }

    // --- minimal request/response shapes for OpenAI's /v1/chat/completions endpoint ---

    public record Message(String role, String content) {}

    private record CompletionRequest(String model, List<Message> messages, double temperature) {}

    private record CompletionResponse(List<Choice> choices) {}

    private record Choice(@JsonProperty("message") Message message) {}
}
