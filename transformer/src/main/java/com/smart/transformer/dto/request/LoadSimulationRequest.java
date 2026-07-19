package com.smart.transformer.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Inputs for the Phase 3 "What-if Load Simulator": lets an engineer ask
 * "what happens to this transformer's health/risk if load increases by X%?".
 */
@Getter
@Setter
public class LoadSimulationRequest {

    @NotNull(message = "Transformer id is required")
    private Long transformerId;

    /** Percentage change in load current vs. the latest reading, e.g. 20 for +20%, -10 for -10%. */
    @NotNull(message = "Load change percent is required")
    private Double loadChangePercent;

    /** Optional: simulate an ambient temperature delta (Celsius) alongside the load change. */
    private Double ambientTempDeltaCelsius = 0.0;
}
