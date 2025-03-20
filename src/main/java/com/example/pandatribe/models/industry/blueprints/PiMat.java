package com.example.pandatribe.models.industry.blueprints;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PiMat {
    private Integer id;
    private String name;
    private Integer type;
    private String icon;
    private BigDecimal price;
    private List<PiDependency> dependencies;
    private Integer quantity;

}
