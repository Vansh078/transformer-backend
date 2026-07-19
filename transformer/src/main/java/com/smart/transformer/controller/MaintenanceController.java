package com.smart.transformer.controller;

import com.smart.transformer.dto.request.MaintenanceRequest;
import com.smart.transformer.dto.response.ApiResponse;
import com.smart.transformer.dto.response.MaintenanceResponse;
import com.smart.transformer.dto.response.MaintenanceSummaryResponse;
import com.smart.transformer.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENGINEER')")
    public ApiResponse<MaintenanceResponse> log(@Valid @RequestBody MaintenanceRequest request) {
        return ApiResponse.success("Maintenance record logged", maintenanceService.log(request));
    }

    @GetMapping("/transformer/{transformerId}")
    public ApiResponse<List<MaintenanceResponse>> getByTransformer(@PathVariable Long transformerId) {
        return ApiResponse.success(maintenanceService.getByTransformer(transformerId));
    }

    /** Phase 2 "AI Maintenance Summary" — OpenAI-generated summary of a transformer's maintenance history. */
    @GetMapping("/transformer/{transformerId}/ai-summary")
    public ApiResponse<MaintenanceSummaryResponse> aiSummary(@PathVariable Long transformerId) {
        return ApiResponse.success(maintenanceService.summarize(transformerId));
    }
}
