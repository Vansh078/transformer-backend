package com.smart.transformer.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smart.transformer.dto.request.ChatRequest;
import com.smart.transformer.dto.response.AlertResponse;
import com.smart.transformer.dto.response.ChatResponse;
import com.smart.transformer.dto.response.SensorReadingResponse;
import com.smart.transformer.entity.Transformer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * "Ask Your Transformer" — Phase 2 feature.
 * Grounds an OpenAI chat completion in the transformer's actual live data
 * (latest reading, open alerts) so answers are specific, not generic.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final WebClient openAiWebClient;
    private final TransformerService transformerService;
    private final SensorReadingService sensorReadingService;
    private final AlertService alertService;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    public ChatResponse ask(ChatRequest request) {
        Transformer transformer = transformerService.getEntity(request.getTransformerId());
        SensorReadingResponse latest = sensorReadingService.getLatest(request.getTransformerId());
        List<AlertResponse> openAlerts = alertService
                .getByTransformer(request.getTransformerId(), PageRequest.of(0, 5))
                .getContent();

        String context = buildContext(transformer, latest, openAlerts);

        String answer = callOpenAi(context, request.getQuestion());

        return new ChatResponse(answer, context);
    }

    private String buildContext(Transformer t, SensorReadingResponse latest, List<AlertResponse> alerts) {
        StringBuilder sb = new StringBuilder();
        sb.append("Transformer: ").append(t.getName()).append(" (").append(t.getAssetTag()).append(")\n");
        sb.append("Location: ").append(t.getLocation() != null ? t.getLocation() : "unknown").append("\n");
        sb.append("Status: ").append(t.getStatus()).append("\n");
        sb.append("Health score: ").append(t.getHealthScore() != null ? t.getHealthScore() : "not yet scored").append("\n");

        if (latest != null) {
            sb.append("Latest reading (").append(latest.getRecordedAt()).append("): ")
                    .append("temp=").append(latest.getTemperatureCelsius()).append("C, ")
                    .append("oil=").append(latest.getOilLevelPercent()).append("%, ")
                    .append("vibration=").append(latest.getVibrationMm()).append("mm, ")
                    .append("load=").append(latest.getLoadCurrentAmps()).append("A, ")
                    .append("anomaly_score=").append(latest.getAnomalyScore()).append("\n");
        } else {
            sb.append("No sensor readings recorded yet.\n");
        }

        if (!alerts.isEmpty()) {
            sb.append("Recent alerts:\n");
            for (AlertResponse a : alerts) {
                sb.append("- [").append(a.getSeverity()).append("] ").append(a.getMessage())
                        .append(a.isAcknowledged() ? " (acknowledged)" : " (open)").append("\n");
            }
        } else {
            sb.append("No alerts on record.\n");
        }

        return sb.toString();
    }

    private String callOpenAi(String context, String question) {
        String systemPrompt = "You are a maintenance assistant for an electrical transformer fleet. "
                + "Answer using ONLY the data provided below. If the data doesn't cover the question, "
                + "say so plainly rather than guessing. Keep answers concise and actionable for a field engineer.\n\n"
                + context;

        ChatCompletionRequest body = new ChatCompletionRequest(
                model,
                List.of(
                        new ChatMessage("system", systemPrompt),
                        new ChatMessage("user", question)
                ),
                0.3
        );

        ChatCompletionResponse response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .block();

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return "I couldn't generate a response right now — please try again.";
        }

        return response.choices().get(0).message().content();
    }

    // --- minimal request/response shapes for OpenAI's /v1/chat/completions endpoint ---

    private record ChatMessage(String role, String content) {}

    private record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            double temperature) {}

    private record ChatCompletionResponse(List<Choice> choices) {}

    private record Choice(@JsonProperty("message") ChatMessage message) {}
}
