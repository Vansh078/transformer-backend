package com.smart.transformer.service;

import com.smart.transformer.dto.request.TransformerRequest;
import com.smart.transformer.dto.response.TransformerResponse;
import com.smart.transformer.entity.Transformer;
import com.smart.transformer.entity.enums.TransformerStatus;
import com.smart.transformer.exception.DuplicateResourceException;
import com.smart.transformer.exception.ResourceNotFoundException;
import com.smart.transformer.repository.TransformerRepository;
import com.smart.transformer.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransformerService {

    private final TransformerRepository transformerRepository;

    @Transactional
    public TransformerResponse create(TransformerRequest request) {
        if (transformerRepository.existsByAssetTag(request.getAssetTag())) {
            throw new DuplicateResourceException("A transformer with asset tag '" + request.getAssetTag() + "' already exists");
        }
        Transformer transformer = new Transformer();
        applyRequest(transformer, request);
        return EntityMapper.toResponse(transformerRepository.save(transformer));
    }

    @Transactional
    public TransformerResponse update(Long id, TransformerRequest request) {
        Transformer transformer = getEntity(id);
        applyRequest(transformer, request);
        return EntityMapper.toResponse(transformerRepository.save(transformer));
    }

    public TransformerResponse getById(Long id) {
        return EntityMapper.toResponse(getEntity(id));
    }

    public Page<TransformerResponse> getAll(Pageable pageable) {
        return transformerRepository.findAll(pageable).map(EntityMapper::toResponse);
    }

    public Page<TransformerResponse> getByStatus(TransformerStatus status, Pageable pageable) {
        return transformerRepository.findByStatus(status, pageable).map(EntityMapper::toResponse);
    }

    @Transactional
    public void delete(Long id) {
        if (!transformerRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Transformer", id);
        }
        transformerRepository.deleteById(id);
    }

    @Transactional
    public void updateHealthScore(Long id, double healthScore, TransformerStatus status) {
        Transformer transformer = getEntity(id);
        transformer.setHealthScore(healthScore);
        transformer.setStatus(status);
        transformerRepository.save(transformer);
    }

    public Transformer getEntity(Long id) {
        return transformerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Transformer", id));
    }

    private void applyRequest(Transformer transformer, TransformerRequest request) {
        transformer.setAssetTag(request.getAssetTag());
        transformer.setName(request.getName());
        transformer.setLocation(request.getLocation());
        transformer.setLatitude(request.getLatitude());
        transformer.setLongitude(request.getLongitude());
        transformer.setCapacityKva(request.getCapacityKva());
        transformer.setInstallationDate(request.getInstallationDate());
    }
}
