package com.example.pandatribe.services.contracts;

import com.example.pandatribe.models.requests.MaterialInfo;

import java.util.List;

public interface MaterialService {
    List<MaterialInfo> getMaterialsByActivity(Integer blueprintId, Integer quantity, Integer discountBR, Integer materialEfficiency, Integer discountB,
                                              Double security, Integer blueprintCount, Integer regionId, Integer initialTier, Long locationId);

    Integer getBlueprintComplexity(Integer blueprintId);
}
