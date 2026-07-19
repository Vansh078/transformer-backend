package com.smart.transformer.util;

import com.smart.transformer.dto.response.*;
import com.smart.transformer.entity.*;

/**
 * Plain hand-written mappers between entities and DTOs.
 * Kept dependency-free (no MapStruct) to keep the build simple for a hackathon-scale project.
 */
public final class EntityMapper {

    private EntityMapper() {}

    public static TransformerResponse toResponse(Transformer t) {
        return new TransformerResponse(
                t.getId(),
                t.getAssetTag(),
                t.getName(),
                t.getLocation(),
                t.getLatitude(),
                t.getLongitude(),
                t.getCapacityKva(),
                t.getInstallationDate(),
                t.getStatus(),
                t.getHealthScore(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    public static DeviceResponse toResponse(Device d) {
        return new DeviceResponse(
                d.getId(),
                d.getDeviceUid(),
                d.getTransformer() != null ? d.getTransformer().getId() : null,
                d.getTransformer() != null ? d.getTransformer().getName() : null,
                d.getFirmwareVersion(),
                d.getStatus(),
                d.getLastSeenAt()
        );
    }

    public static SensorReadingResponse toResponse(SensorReading r) {
        return new SensorReadingResponse(
                r.getId(),
                r.getTransformer() != null ? r.getTransformer().getId() : null,
                r.getDevice() != null ? r.getDevice().getDeviceUid() : null,
                r.getTemperatureCelsius(),
                r.getVibrationMm(),
                r.getLoadCurrentAmps(),
                r.getVoltageVolts(),
                r.getAnomalyScore(),
                r.getRecordedAt()
        );
    }

    public static AlertResponse toResponse(Alert a) {
        return new AlertResponse(
                a.getId(),
                a.getTransformer() != null ? a.getTransformer().getId() : null,
                a.getTransformer() != null ? a.getTransformer().getName() : null,
                a.getSeverity(),
                a.getSource(),
                a.getMessage(),
                a.isAcknowledged(),
                a.getResolvedAt(),
                a.getCreatedAt(),
                a.getNarrative(),
                a.getExplanation()
        );
    }

    public static MaintenanceResponse toResponse(MaintenanceRecord m) {
        return new MaintenanceResponse(
                m.getId(),
                m.getTransformer() != null ? m.getTransformer().getId() : null,
                m.getDescription(),
                m.getPerformedBy(),
                m.getPerformedAt(),
                m.getNextDueAt()
        );
    }
}
