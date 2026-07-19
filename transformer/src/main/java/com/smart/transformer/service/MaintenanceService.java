package com.smart.transformer.service;

import com.smart.transformer.dto.request.MaintenanceRequest;
import com.smart.transformer.dto.response.MaintenanceResponse;
import com.smart.transformer.dto.response.MaintenanceSummaryResponse;
import com.smart.transformer.entity.MaintenanceRecord;
import com.smart.transformer.entity.Transformer;
import com.smart.transformer.repository.MaintenanceRecordRepository;
import com.smart.transformer.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final TransformerService transformerService;
    private final OpenAiService openAiService;
    private final ActivityLogService activityLogService;

    @Transactional
    public MaintenanceResponse log(MaintenanceRequest request) {
        Transformer transformer = transformerService.getEntity(request.getTransformerId());

        MaintenanceRecord record = new MaintenanceRecord();
        record.setTransformer(transformer);
        record.setDescription(request.getDescription());
        record.setPerformedBy(request.getPerformedBy());
        record.setPerformedAt(request.getPerformedAt() != null ? request.getPerformedAt() : Instant.now());
        record.setNextDueAt(request.getNextDueAt());

        MaintenanceResponse response = EntityMapper.toResponse(maintenanceRecordRepository.save(record));
        activityLogService.record("MAINTENANCE_LOGGED", "Transformer", transformer.getId(), request.getDescription());
        return response;
    }

    public List<MaintenanceResponse> getByTransformer(Long transformerId) {
        return maintenanceRecordRepository.findByTransformerIdOrderByPerformedAtDesc(transformerId)
                .stream().map(EntityMapper::toResponse).toList();
    }

    /**
     * Phase 2 "AI Maintenance Summary" — asks OpenAI to condense a transformer's
     * maintenance history into a short summary for reports/dashboards, highlighting
     * recurring issues and what's due next.
     */
    public MaintenanceSummaryResponse summarize(Long transformerId) {
        Transformer transformer = transformerService.getEntity(transformerId);
        List<MaintenanceRecord> records = maintenanceRecordRepository
                .findByTransformerIdOrderByPerformedAtDesc(transformerId);

        if (records.isEmpty()) {
            return new MaintenanceSummaryResponse(transformer.getId(), transformer.getName(), 0,
                    "No maintenance records exist for this transformer yet.");
        }

        StringBuilder history = new StringBuilder();
        for (MaintenanceRecord r : records) {
            history.append("- ").append(r.getPerformedAt()).append(": ").append(r.getDescription());
            if (r.getPerformedBy() != null) {
                history.append(" (by ").append(r.getPerformedBy()).append(")");
            }
            if (r.getNextDueAt() != null) {
                history.append(" [next due: ").append(r.getNextDueAt()).append("]");
            }
            history.append("\n");
        }

        String system = "You summarize transformer maintenance history for engineers and managers. "
                + "Given the chronological maintenance log below for " + transformer.getName()
                + " (" + transformer.getAssetTag() + "), write a concise summary (4-6 sentences) covering: "
                + "overall maintenance frequency, any recurring issues/patterns, and what is next due. "
                + "Only use the data provided.\n\n" + history;

        String summary = openAiService.complete(system, "Summarize this maintenance history.");

        return new MaintenanceSummaryResponse(transformer.getId(), transformer.getName(), records.size(), summary);
    }
}
