package com.smart.transformer.repository;

import com.smart.transformer.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceUid(String deviceUid);
    
    @Query("""
            SELECT d
            FROM Device d
            JOIN FETCH d.transformer
            WHERE d.transformer.id = :transformerId
        """)
        List<Device> findByTransformerId(@Param("transformerId") Long transformerId);
    List<Device> findTop10ByDeviceUidContainingIgnoreCase(String deviceUid);
}
