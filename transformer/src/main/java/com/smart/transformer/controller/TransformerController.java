package com.smart.transformer.controller;

import com.smart.transformer.dto.request.TransformerRequest;
import com.smart.transformer.dto.response.ApiResponse;
import com.smart.transformer.dto.response.PagedResponse;
import com.smart.transformer.dto.response.TransformerResponse;
import com.smart.transformer.entity.enums.TransformerStatus;
import com.smart.transformer.service.TransformerService;
import com.smart.transformer.util.PageUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transformers")
@RequiredArgsConstructor
public class TransformerController {

    private final TransformerService transformerService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENGINEER')")
    public ApiResponse<TransformerResponse> create(@Valid @RequestBody TransformerRequest request) {
        return ApiResponse.success("Transformer created", transformerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ENGINEER')")
    public ApiResponse<TransformerResponse> update(@PathVariable Long id, @Valid @RequestBody TransformerRequest request) {
        return ApiResponse.success("Transformer updated", transformerService.update(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TransformerResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(transformerService.getById(id));
    }

    @GetMapping
    public ApiResponse<PagedResponse<TransformerResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) TransformerStatus status) {

        Pageable pageable = PageUtil.of(page, size, sortBy, direction);
        var result = status != null
                ? transformerService.getByStatus(status, pageable)
                : transformerService.getAll(pageable);

        return ApiResponse.success(PagedResponse.from(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        transformerService.delete(id);
    }
}
