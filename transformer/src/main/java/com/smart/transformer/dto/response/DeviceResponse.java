package com.smart.transformer.dto.response;

import com.smart.transformer.entity.enums.DeviceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponse {
    private Long id;
    private String deviceUid;
    private Long transformerId;
    private String transformerName;
    private String firmwareVersion;
    private DeviceStatus status;
    private Instant lastSeenAt;
}
