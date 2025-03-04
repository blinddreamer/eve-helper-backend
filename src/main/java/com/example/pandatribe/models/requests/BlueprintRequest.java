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
}
