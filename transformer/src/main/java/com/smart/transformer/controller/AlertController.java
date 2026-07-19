package com.smart.transformer.controller;

import com.smart.transformer.dto.request.AlertRequest;
import com.smart.transformer.dto.response.AlertResponse;
import com.smart.transformer.dto.response.ApiResponse;
import com.smart.transformer.dto.response.PagedResponse;
import com.smart.transformer.service.AlertService;
import com.smart.transformer.util.PageUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @PostMapping
    //@PreAuthorize("hasRole('ADMIN') or hasRole('ENGINEER')")
    public ApiResponse<AlertResponse> raise(@Valid @RequestBody AlertRequest request) {
        return ApiResponse.success("Alert raised", alertService.raise(request));
    }

    @PatchMapping("/{id}/acknowledge")
    //@PreAuthorize("hasRole('ADMIN') or hasRole('ENGINEER')")
    public ApiResponse<AlertResponse> acknowledge(@PathVariable Long id) {
        return ApiResponse.success("Alert acknowledged", alertService.acknowledge(id));
    }

    @GetMapping("/open")
    public ApiResponse<PagedResponse<AlertResponse>> getOpen(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageUtil.of(page, size, "createdAt", "desc");
        return ApiResponse.success(PagedResponse.from(alertService.getOpenAlerts(pageable)));
    }

    @GetMapping("/transformer/{transformerId}")
    public ApiResponse<PagedResponse<AlertResponse>> getByTransformer(
            @PathVariable Long transformerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageUtil.of(page, size, "createdAt", "desc");
        return ApiResponse.success(PagedResponse.from(alertService.getByTransformer(transformerId, pageable)));
    }

    /**
     * Explainable AI + Automatic Incident Narratives — (re)generates the AI-authored
     * narrative and explanation for an alert on demand, in case the automatic
     * background enrichment hasn't finished yet or needs a refresh.
     */
    @PostMapping("/{id}/explain")
    public ApiResponse<AlertResponse> explain(@PathVariable Long id) {
        return ApiResponse.success(alertService.explain(id));
    }
}
