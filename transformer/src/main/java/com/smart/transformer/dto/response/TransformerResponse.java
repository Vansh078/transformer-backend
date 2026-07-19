package com.smart.transformer.dto.response;

import com.smart.transformer.entity.enums.TransformerStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransformerResponse {
    private Long id;
    private String assetTag;
    private String name;
    private String location;
    private Double latitude;
    private Double longitude;
    private BigDecimal capacityKva;
    private LocalDate installationDate;
    private TransformerStatus status;
    private Double healthScore;
    private Instant createdAt;
    private Instant updatedAt;
}
