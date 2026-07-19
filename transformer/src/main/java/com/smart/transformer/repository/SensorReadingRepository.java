package com.smart.transformer.repository;

import com.smart.transformer.entity.SensorReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    Page<SensorReading> findByTransformerIdOrderByRecordedAtDesc(Long transformerId, Pageable pageable);

    List<SensorReading> findByTransformerIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long transformerId, Instant from, Instant to);

    SensorReading findFirstByTransformerIdOrderByRecordedAtDesc(Long transformerId);
}
