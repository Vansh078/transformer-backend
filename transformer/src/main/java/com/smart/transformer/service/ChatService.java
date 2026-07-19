package com.smart.transformer.service;

import com.smart.transformer.dto.request.ChatRequest;
import com.smart.transformer.dto.response.AlertResponse;
import com.smart.transformer.dto.response.ChatConversationResponse;
import com.smart.transformer.dto.response.ChatMessageResponse;
import com.smart.transformer.dto.response.ChatResponse;
import com.smart.transformer.dto.response.PagedResponse;
import com.smart.transformer.dto.response.SensorReadingResponse;
import com.smart.transformer.entity.ChatConversation;
import com.smart.transformer.entity.ChatMessage;
import com.smart.transformer.entity.Transformer;
import com.smart.transformer.entity.enums.ChatRole;
import com.smart.transformer.exception.ResourceNotFoundException;
import com.smart.transformer.repository.ChatConversationRepository;
import com.smart.transformer.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * "Ask Your Transformer" — Phase 2 LLM chat feature, backed by OpenAI.
 * Grounds every answer in the transformer's actual live data (latest reading, open
 * alerts) and persists conversation history so follow-up questions keep context,
 * just like a normal chat assistant.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_HISTORY_MESSAGES = 20; // cap sent to the LLM to bound token usage

    private final OpenAiService openAiService;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final TransformerService transformerService;
    private final SensorReadingService sensorReadingService;
    private final AlertService alertService;
    private final ActivityLogService activityLogService;

    @Transactional
    public ChatResponse ask(ChatRequest request) {
        Transformer transformer = transformerService.getEntity(request.getTransformerId());

        ChatConversation conversation = request.getConversationId() != null
                ? getOwnedConversation(request.getConversationId(), transformer.getId())
                : startConversation(transformer);

        String context = buildContext(transformer);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setConversation(conversation);
        userMessage.setRole(ChatRole.USER);
        userMessage.setContent(request.getQuestion());
        messageRepository.save(userMessage);

        String answer = callOpenAi(conversation, context);

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setConversation(conversation);
        assistantMessage.setRole(ChatRole.ASSISTANT);
        assistantMessage.setContent(answer);
        messageRepository.save(assistantMessage);

        conversation.setUpdatedAt(java.time.Instant.now());
        conversationRepository.save(conversation);

        activityLogService.record("CHAT_ASK", "Transformer", transformer.getId(),
                "conversationId=" + conversation.getId());

        return new ChatResponse(conversation.getId(), answer, context);
    }

    public PagedResponse<ChatConversationResponse> getConversations(Long transformerId, Pageable pageable) {
        return PagedResponse.from(
                conversationRepository.findByTransformerIdOrderByUpdatedAtDesc(transformerId, pageable)
                        .map(c -> toSummary(c)));
    }

    public ChatConversationResponse getConversation(Long conversationId) {
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Chat conversation", conversationId));
        List<ChatMessageResponse> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toResponse)
                .toList();
        ChatConversationResponse response = toSummary(conversation);
        response.setMessages(messages);
        return response;
    }

    private ChatConversation startConversation(Transformer transformer) {
        ChatConversation conversation = new ChatConversation();
        conversation.setTransformer(transformer);
        conversation.setUserId(currentUserId());
        conversation.setTitle("Chat about " + transformer.getName());
        return conversationRepository.save(conversation);
    }

    private ChatConversation getOwnedConversation(Long conversationId, Long transformerId) {
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Chat conversation", conversationId));
        if (conversation.getTransformer() == null || !conversation.getTransformer().getId().equals(transformerId)) {
            throw ResourceNotFoundException.of("Chat conversation", conversationId);
        }
        return conversation;
    }

    private String buildContext(Transformer t) {
        SensorReadingResponse latest = sensorReadingService.getLatest(t.getId());
        List<AlertResponse> alerts = alertService.getByTransformer(t.getId(), PageRequest.of(0, 5)).getContent();

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

    private String callOpenAi(ChatConversation conversation, String context) {
        String systemPrompt = "You are a maintenance assistant for an electrical transformer fleet. "
                + "Answer using ONLY the data provided below plus the conversation history. If the data "
                + "doesn't cover the question, say so plainly rather than guessing. Keep answers concise "
                + "and actionable for a field engineer.\n\n" + context;

        List<OpenAiService.Message> messages = new java.util.ArrayList<>();
        messages.add(new OpenAiService.Message("system", systemPrompt));

        // history already includes the just-persisted user message for this turn
        List<ChatMessage> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        int fromIndex = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        for (ChatMessage m : history.subList(fromIndex, history.size())) {
            messages.add(new OpenAiService.Message(m.getRole() == ChatRole.ASSISTANT ? "assistant" : "user", m.getContent()));
        }

        return openAiService.complete(messages, 0.3);
    }

    private ChatConversationResponse toSummary(ChatConversation c) {
        return new ChatConversationResponse(
                c.getId(),
                c.getTransformer() != null ? c.getTransformer().getId() : null,
                c.getTransformer() != null ? c.getTransformer().getName() : null,
                c.getTitle(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                null
        );
    }

    private ChatMessageResponse toResponse(ChatMessage m) {
        return new ChatMessageResponse(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt());
    }

    private UUID currentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                return UUID.fromString(jwt.getSubject());
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
