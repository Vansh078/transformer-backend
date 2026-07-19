package com.smart.transformer.controller;

import com.smart.transformer.dto.request.FailureCostRequest;
import com.smart.transformer.dto.request.LoadSimulationRequest;
import com.smart.transformer.dto.response.*;
import com.smart.transformer.service.DecisionSupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Phase 3 — Decision Support: failure cost calculator, load simulator, risk heatmap, comparison. */
@RestController
@RequestMapping("/api/v1/decision-support")
@RequiredArgsConstructor
public class DecisionSupportController {

    private final DecisionSupportService decisionSupportService;

    @PostMapping("/failure-cost")
    public ApiResponse<FailureCostResponse> failureCost(@Valid @RequestBody FailureCostRequest request) {
        return ApiResponse.success(decisionSupportService.calculateFailureCost(request));
    }

    @PostMapping("/load-simulation")
    public ApiResponse<LoadSimulationResponse> loadSimulation(@Valid @RequestBody LoadSimulationRequest request) {
        return ApiResponse.success(decisionSupportService.simulateLoad(request));
    }

    @GetMapping("/fleet-risk-heatmap")
    public ApiResponse<List<FleetRiskPointResponse>> fleetRiskHeatmap() {
        return ApiResponse.success(decisionSupportService.getFleetRiskHeatmap());
    }

    @GetMapping("/compare")
    public ApiResponse<TransformerComparisonResponse> compare(@RequestParam List<Long> transformerIds) {
        return ApiResponse.success(decisionSupportService.compare(transformerIds));
    }
}
