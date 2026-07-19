package com.smart.transformer.service;

import com.smart.transformer.dto.request.DeviceRequest;
import com.smart.transformer.dto.response.DeviceResponse;
import com.smart.transformer.entity.Device;
import com.smart.transformer.entity.Transformer;
import com.smart.transformer.entity.enums.DeviceStatus;
import com.smart.transformer.exception.DuplicateResourceException;
import com.smart.transformer.exception.ResourceNotFoundException;
import com.smart.transformer.repository.DeviceRepository;
import com.smart.transformer.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final TransformerService transformerService;

    @Transactional
    public DeviceResponse register(DeviceRequest request) {
        if (deviceRepository.findByDeviceUid(request.getDeviceUid()).isPresent()) {
            throw new DuplicateResourceException("Device '" + request.getDeviceUid() + "' is already registered");
        }
        Transformer transformer = transformerService.getEntity(request.getTransformerId());

        Device device = new Device();
        device.setDeviceUid(request.getDeviceUid());
        device.setTransformer(transformer);
        device.setFirmwareVersion(request.getFirmwareVersion());
        device.setStatus(DeviceStatus.OFFLINE);

        return EntityMapper.toResponse(deviceRepository.save(device));
    }

    public List<DeviceResponse> getByTransformer(Long transformerId) {
        return deviceRepository.findByTransformerId(transformerId).stream()
                .map(EntityMapper::toResponse)
                .toList();
    }

    @Transactional
    public void heartbeat(String deviceUid) {
        Device device = deviceRepository.findByDeviceUid(deviceUid)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown device: " + deviceUid));
        device.setStatus(DeviceStatus.ONLINE);
        device.setLastSeenAt(Instant.now());
        deviceRepository.save(device);
    }

    Device getEntityByUid(String deviceUid) {
        return deviceRepository.findByDeviceUid(deviceUid)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown device: " + deviceUid));
    }
}
