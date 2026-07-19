package com.smart.transformer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceRequest {

    @NotBlank(message = "Device UID is required")
    private String deviceUid;

    @NotNull(message = "Transformer id is required")
    private Long transformerId;

    private String firmwareVersion;
}
