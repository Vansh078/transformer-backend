package com.smart.transformer.repository;

import com.smart.transformer.entity.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {
    List<MaintenanceRecord> findByTransformerIdOrderByPerformedAtDesc(Long transformerId);
    long countByTransformerId(Long transformerId);
}
