package com.smart.transformer.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

/**
 * Inputs for the Phase 3 "Failure Cost Calculator". All costs are in the caller's
 * chosen currency; the calculator just does the arithmetic and risk weighting.
 */
@Getter
@Setter
public class FailureCostRequest {

    @NotNull(message = "Transformer id is required")
    private Long transformerId;

    /** Cost to replace/repair the transformer outright if it fails unexpectedly. */
    @NotNull(message = "Replacement cost is required")
    @PositiveOrZero
    private Double replacementCost;

    /** Estimated revenue/production loss per hour of downtime. */
    @NotNull(message = "Downtime cost per hour is required")
    @PositiveOrZero
    private Double downtimeCostPerHour;

    /** Estimated hours of downtime if this transformer fails unexpectedly. */
    @NotNull(message = "Estimated downtime hours is required")
    @PositiveOrZero
    private Double estimatedDowntimeHours;

    /** Cost of proactive/preventive maintenance to avoid the failure. */
    @PositiveOrZero
    private Double preventiveMaintenanceCost = 0.0;
}
