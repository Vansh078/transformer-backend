package com.smart.transformer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Phase 4 "Global Search" — one query fanned out across the fleet's core entities. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchResponse {
    private String query;
    private List<TransformerResponse> transformers;
    private List<DeviceResponse> devices;
    private List<AlertResponse> alerts;
}
