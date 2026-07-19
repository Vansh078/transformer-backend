package com.smart.transformer.repository;

import com.smart.transformer.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceUid(String deviceUid);
    List<Device> findByTransformerId(Long transformerId);
    List<Device> findTop10ByDeviceUidContainingIgnoreCase(String deviceUid);
}
