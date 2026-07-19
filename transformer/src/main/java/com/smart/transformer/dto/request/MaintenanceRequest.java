package com.smart.transformer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class MaintenanceRequest {

    @NotNull(message = "Transformer id is required")
    private Long transformerId;

    @NotBlank(message = "Description is required")
    private String description;

    private String performedBy;

    private Instant performedAt;

    private Instant nextDueAt;
}
