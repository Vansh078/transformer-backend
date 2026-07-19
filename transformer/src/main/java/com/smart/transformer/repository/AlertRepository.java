package com.smart.transformer.repository;

import com.smart.transformer.entity.Alert;
import com.smart.transformer.entity.enums.AlertSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    Page<Alert> findByAcknowledgedFalseOrderByCreatedAtDesc(Pageable pageable);
    Page<Alert> findByTransformerIdOrderByCreatedAtDesc(Long transformerId, Pageable pageable);
    Page<Alert> findBySeverityOrderByCreatedAtDesc(AlertSeverity severity, Pageable pageable);
    long countByAcknowledgedFalseAndSeverity(AlertSeverity severity);
}
