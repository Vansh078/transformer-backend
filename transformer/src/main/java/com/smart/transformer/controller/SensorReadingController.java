package com.smart.transformer.controller;

import com.smart.transformer.dto.request.SensorReadingRequest;
import com.smart.transformer.dto.response.ApiResponse;
import com.smart.transformer.dto.response.PagedResponse;
import com.smart.transformer.dto.response.SensorReadingResponse;
import com.smart.transformer.service.SensorReadingService;
import com.smart.transformer.util.PageUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/readings")
@RequiredArgsConstructor
public class SensorReadingController {

    private final SensorReadingService sensorReadingService;

    /**
     * Public-ish endpoint (permitted in SecurityConfig) — ESP32 devices post here directly.
     * In production, protect this with a per-device shared secret header rather than a user JWT.
     */
    @PostMapping("/ingest")
    public ApiResponse<SensorReadingResponse> ingest(@Valid @RequestBody SensorReadingRequest request) {
        return ApiResponse.success(sensorReadingService.ingest(request));
    }

    @GetMapping("/transformer/{transformerId}")
    public ApiResponse<PagedResponse<SensorReadingResponse>> getHistory(
            @PathVariable Long transformerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageUtil.of(page, size, "recordedAt", "desc");
        return ApiResponse.success(PagedResponse.from(sensorReadingService.getHistory(transformerId, pageable)));
    }

    @GetMapping("/transformer/{transformerId}/range")
    public ApiResponse<List<SensorReadingResponse>> getRange(
            @PathVariable Long transformerId,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        return ApiResponse.success(sensorReadingService.getRange(transformerId, from, to));
    }

    @GetMapping("/transformer/{transformerId}/latest")
    public ApiResponse<SensorReadingResponse> getLatest(@PathVariable Long transformerId) {
        return ApiResponse.success(sensorReadingService.getLatest(transformerId));
    }
}
