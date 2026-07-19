package com.smart.transformer.repository;

import com.smart.transformer.entity.Report;
import com.smart.transformer.entity.enums.ReportStatus;
import com.smart.transformer.entity.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    Page<Report> findByTransformerIdOrderByGeneratedAtDesc(Long transformerId, Pageable pageable);

    Report findFirstByOrderByGeneratedAtDesc();

    Report findFirstByTransformerIdOrderByGeneratedAtDesc(Long transformerId);

    long countByStatus(ReportStatus status);

    long countByReportType(ReportType reportType);

    long countByGeneratedAtBetween(Instant from, Instant to);

    List<Report> findByStatusAndGeneratedAtBefore(ReportStatus status, Instant cutoff);
}
