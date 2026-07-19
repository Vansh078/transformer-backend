package com.smart.transformer.dto.response;

import com.smart.transformer.entity.enums.AlertSeverity;
import com.smart.transformer.entity.enums.AlertSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private Long id;
    private Long transformerId;
    private String transformerName;
    private AlertSeverity severity;
    private AlertSource source;
    private String message;
    private boolean acknowledged;
    private Instant resolvedAt;
    private Instant createdAt;
    private String narrative;
    private String explanation;
}
