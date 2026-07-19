package com.smart.transformer.service;

import com.smart.transformer.dto.request.MaintenanceRequest;
import com.smart.transformer.dto.response.MaintenanceResponse;
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

    @Transactional
    public MaintenanceResponse log(MaintenanceRequest request) {
        Transformer transformer = transformerService.getEntity(request.getTransformerId());

        MaintenanceRecord record = new MaintenanceRecord();
        record.setTransformer(transformer);
        record.setDescription(request.getDescription());
        record.setPerformedBy(request.getPerformedBy());
        record.setPerformedAt(request.getPerformedAt() != null ? request.getPerformedAt() : Instant.now());
        record.setNextDueAt(request.getNextDueAt());

        return EntityMapper.toResponse(maintenanceRecordRepository.save(record));
    }

    public List<MaintenanceResponse> getByTransformer(Long transformerId) {
        return maintenanceRecordRepository.findByTransformerIdOrderByPerformedAtDesc(transformerId)
                .stream().map(EntityMapper::toResponse).toList();
    }
}
