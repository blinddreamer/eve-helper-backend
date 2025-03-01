package com.example.pandatribe.models.requests;

import lombok.*;

@Builder
@AllArgsConstructor
@Data
public class BlueprintRequest {
    private String blueprintName;
    private Integer runs;
    private Integer blueprintMe;
    private String system;
    private Double facilityTax;
    private Integer buildingRig;
    private Integer building;
    private Integer count;
    private Integer regionId;
    private Boolean init;
    private String requestId;
    private Integer tier;

    public String cacheKey() {
        return String.join("|",
                blueprintName,
                String.valueOf(runs),
                String.valueOf(count),
                system,
                String.valueOf(blueprintMe),
                String.valueOf(facilityTax),
                String.valueOf(buildingRig),
                String.valueOf(building),
                String.valueOf(regionId)
        );
    }
}
