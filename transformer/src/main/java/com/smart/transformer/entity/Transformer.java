package com.smart.transformer.entity;

import com.smart.transformer.entity.enums.TransformerStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "transformers")
public class Transformer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_tag", nullable = false, unique = true, length = 50)
    private String assetTag;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String location;

    private Double latitude;

    private Double longitude;

    @Column(name = "capacity_kva")
    private BigDecimal capacityKva;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransformerStatus status = TransformerStatus.HEALTHY;

    @Column(name = "health_score")
    private Double healthScore;
}
