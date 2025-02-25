package com.example.pandatribe.models.results;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlueprintResult {
    private Integer id;
    private String name;
    private Integer quantity;
    private Integer jobsCount;
    private String icon;
    private BigDecimal craftPrice;
    private BigDecimal sellPrice;
    private BigDecimal totalSellPrice;
    private List<BlueprintResult> materialsList;
    private BigDecimal adjustedPrice;
    private Double excessMaterials;
    private Double volume;
    private Double totalVolume;
    private Boolean isCreatable;
    private BigDecimal industryCosts;
    private Integer activityId;
    private Double craftQuantity;
    private Boolean isFuel;
    private Integer tier;
    private Boolean selectedForCraft;
}
