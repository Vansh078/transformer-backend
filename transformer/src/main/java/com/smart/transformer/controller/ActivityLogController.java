package com.smart.transformer.controller;

import com.smart.transformer.dto.response.ActivityLogResponse;
import com.smart.transformer.dto.response.ApiResponse;
import com.smart.transformer.dto.response.PagedResponse;
import com.smart.transformer.service.ActivityLogService;
import com.smart.transformer.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Phase 4 — "Activity Logs" audit trail viewer. */
@RestController
@RequestMapping("/api/v1/activity-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    public ApiResponse<PagedResponse<ActivityLogResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageUtil.of(page, size, "createdAt", "desc");
        return ApiResponse.success(activityLogService.getAll(pageable));
    }

    @GetMapping("/entity/{entityType}")
    public ApiResponse<PagedResponse<ActivityLogResponse>> getByEntity(
            @PathVariable String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageUtil.of(page, size, "createdAt", "desc");
        return ApiResponse.success(activityLogService.getByEntity(entityType, entityId, pageable));
    }
}
