package com.smart.transformer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequest {

    @NotNull(message = "Transformer id is required")
    private Long transformerId;

    @NotBlank(message = "Question is required")
    private String question;

    /**
     * Optional. If omitted, a new conversation is started. If provided, the question
     * is appended to that conversation's history so the LLM keeps prior turns in context.
     */
    private Long conversationId;
}
