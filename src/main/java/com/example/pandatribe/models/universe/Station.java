package com.example.pandatribe.models.universe;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Station {
    private Long stationId;
    private String stationName;
    private Integer regionId;
    private String regionName;
}
