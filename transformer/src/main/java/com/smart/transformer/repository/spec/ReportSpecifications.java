package com.smart.transformer.repository.spec;

import com.smart.transformer.entity.Report;
import com.smart.transformer.entity.enums.ReportType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/**
 * Composable filters for the report history API (transformer / report type / date range).
 * Kept as plain Specifications (rather than Querydsl) to match this project's
 * "hand-written, dependency-light" convention (see EntityMapper).
 */
public final class ReportSpecifications {

    private ReportSpecifications() {}

    public static Specification<Report> transformerId(Long transformerId) {
        return (root, query, cb) -> transformerId == null ? null
                : cb.equal(root.get("transformer").get("id"), transformerId);
    }

    public static Specification<Report> reportType(ReportType reportType) {
        return (root, query, cb) -> reportType == null ? null
                : cb.equal(root.get("reportType"), reportType);
    }

    public static Specification<Report> generatedAfter(Instant from) {
        return (root, query, cb) -> from == null ? null
                : cb.greaterThanOrEqualTo(root.get("generatedAt"), from);
    }

    public static Specification<Report> generatedBefore(Instant to) {
        return (root, query, cb) -> to == null ? null
                : cb.lessThanOrEqualTo(root.get("generatedAt"), to);
    }

    public static Specification<Report> withFilters(Long transformerId, ReportType reportType, Instant from, Instant to) {
        return Specification.where(transformerId(transformerId))
                .and(reportType(reportType))
                .and(generatedAfter(from))
                .and(generatedBefore(to));
    }
}
