package com.smart.transformer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponse {
    private Long id;
    private UUID userId;
    private String actorLabel;
    private String action;
    private String entityType;
    private String entityId;
    private String details;
    private Instant createdAt;
}
