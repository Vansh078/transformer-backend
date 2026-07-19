package com.smart.transformer.service;

import com.smart.transformer.dto.request.SensorReadingRequest;
import com.smart.transformer.dto.response.SensorReadingResponse;
import com.smart.transformer.entity.Device;
import com.smart.transformer.entity.SensorReading;
import com.smart.transformer.entity.Transformer;
import com.smart.transformer.repository.SensorReadingRepository;
import com.smart.transformer.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SensorReadingService {

    private final SensorReadingRepository sensorReadingRepository;
    private final DeviceService deviceService;
    private final HealthScoreService healthScoreService;

    /**
     * Ingests a reading from an ESP32 device: persists it, marks the device online,
     * then hands off to the AI sidecar for anomaly scoring / health recalculation.
     */
    @Transactional
    public SensorReadingResponse ingest(SensorReadingRequest request) {
        Device device = deviceService.getEntityByUid(request.getDeviceUid());
        Transformer transformer = device.getTransformer();

        SensorReading reading = new SensorReading();
        reading.setTransformer(transformer);
        reading.setDevice(device);
        reading.setTemperatureCelsius(request.getTemperatureCelsius());
        reading.setVibrationMm(request.getVibrationMm());
        reading.setLoadCurrentAmps(request.getLoadCurrentAmps());
        reading.setVoltageVolts(request.getVoltageVolts());
        reading.setRecordedAt(request.getRecordedAt() != null ? request.getRecordedAt() : Instant.now());

        SensorReading saved = sensorReadingRepository.save(reading);

        deviceService.heartbeat(request.getDeviceUid());

        // fire-and-forget style call to the AI sidecar; failures here shouldn't break ingestion
        healthScoreService.scoreAsync(saved);

        return EntityMapper.toResponse(saved);
    }

    public Page<SensorReadingResponse> getHistory(Long transformerId, Pageable pageable) {
        return sensorReadingRepository.findByTransformerIdOrderByRecordedAtDesc(transformerId, pageable)
                .map(EntityMapper::toResponse);
    }

    public List<SensorReadingResponse> getRange(Long transformerId, Instant from, Instant to) {
        return sensorReadingRepository
                .findByTransformerIdAndRecordedAtBetweenOrderByRecordedAtAsc(transformerId, from, to)
                .stream().map(EntityMapper::toResponse).toList();
    }

    public SensorReadingResponse getLatest(Long transformerId) {
        SensorReading latest = sensorReadingRepository.findFirstByTransformerIdOrderByRecordedAtDesc(transformerId);
        return latest != null ? EntityMapper.toResponse(latest) : null;
    }
}
