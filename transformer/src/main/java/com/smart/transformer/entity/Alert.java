package com.smart.transformer.entity;

import com.smart.transformer.entity.enums.AlertSeverity;
import com.smart.transformer.entity.enums.AlertSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "alerts")
public class Alert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transformer_id", nullable = false)
    private Transformer transformer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertSource source;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private boolean acknowledged = false;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /** LLM-authored plain-English incident narrative — "Automatic Incident Narratives" feature. */
    @Column(columnDefinition = "TEXT")
    private String narrative;

    /** LLM-authored explanation of why the alert fired — "Explainable AI" feature. */
    @Column(columnDefinition = "TEXT")
    private String explanation;
}
