package com.smart.transformer.repository;

import com.smart.transformer.entity.Transformer;
import com.smart.transformer.entity.enums.TransformerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransformerRepository extends JpaRepository<Transformer, Long> {
    Optional<Transformer> findByAssetTag(String assetTag);
    Page<Transformer> findByStatus(TransformerStatus status, Pageable pageable);
    boolean existsByAssetTag(String assetTag);

    List<Transformer> findTop10ByNameContainingIgnoreCaseOrAssetTagContainingIgnoreCaseOrLocationContainingIgnoreCase(
            String name, String assetTag, String location);
}
