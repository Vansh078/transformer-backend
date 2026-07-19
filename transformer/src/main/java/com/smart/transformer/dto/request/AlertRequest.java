package com.smart.transformer.dto.request;

import com.smart.transformer.entity.enums.AlertSeverity;
import com.smart.transformer.entity.enums.AlertSource;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlertRequest {

    @NotNull(message = "Transformer id is required")
    private Long transformerId;

    @NotNull(message = "Severity is required")
    private AlertSeverity severity;

    @NotNull(message = "Source is required")
    private AlertSource source;

    private String message;
}
