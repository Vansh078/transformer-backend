package com.smart.transformer.service;

import com.smart.transformer.dto.response.GlobalSearchResponse;
import com.smart.transformer.repository.AlertRepository;
import com.smart.transformer.repository.DeviceRepository;
import com.smart.transformer.repository.TransformerRepository;
import com.smart.transformer.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Phase 4 "Global Search" — fans a single query out across transformers, devices, and alerts. */
@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    private final TransformerRepository transformerRepository;
    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;

    @Transactional(readOnly = true)
    public GlobalSearchResponse search(String query) {
        String q = query == null ? "" : query.trim();

        var transformers = transformerRepository
                .findTop10ByNameContainingIgnoreCaseOrAssetTagContainingIgnoreCaseOrLocationContainingIgnoreCase(q, q, q)
                .stream().map(EntityMapper::toResponse).toList();

        var devices = deviceRepository.findTop10ByDeviceUidContainingIgnoreCase(q)
                .stream().map(EntityMapper::toResponse).toList();

        var alerts = alertRepository.findTop10ByMessageContainingIgnoreCase(q)
                .stream().map(EntityMapper::toResponse).toList();

        return new GlobalSearchResponse(q, transformers, devices, alerts);
    }
}