package com.smart.transformer.controller;

import com.smart.transformer.dto.request.DeviceRequest;
import com.smart.transformer.dto.response.ApiResponse;
import com.smart.transformer.dto.response.DeviceResponse;
import com.smart.transformer.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    //@PreAuthorize("hasRole('ADMIN') or hasRole('ENGINEER')")
    public ApiResponse<DeviceResponse> register(@Valid @RequestBody DeviceRequest request) {
        return ApiResponse.success("Device registered", deviceService.register(request));
    }

    @GetMapping("/transformer/{transformerId}")
    public ApiResponse<List<DeviceResponse>> getByTransformer(@PathVariable Long transformerId) {
        return ApiResponse.success(deviceService.getByTransformer(transformerId));
    }
}
