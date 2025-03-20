package com.example.pandatribe.models.industry.blueprints;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PiDependency {
    private Integer typeID;
    private Boolean isInput;
    private Integer quantity;
}
