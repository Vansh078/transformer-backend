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
}
