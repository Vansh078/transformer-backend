package com.smart.transformer.controller;

import com.smart.transformer.dto.response.ApiResponse;
import com.smart.transformer.dto.response.GlobalSearchResponse;
import com.smart.transformer.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Phase 4 "Global Search". */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final GlobalSearchService globalSearchService;

    @GetMapping
    public ApiResponse<GlobalSearchResponse> search(@RequestParam String q) {
        return ApiResponse.success(globalSearchService.search(q));
    }
}
