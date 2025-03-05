package com.example.pandatribe.models.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaterialInfo {
    private Integer id;
    private String icon;
    private String name;
    private Integer quantity;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private Double volume;
    private Boolean isCreatable;
    private BigDecimal adjustedPrice;
    private Integer tier;
    private Integer activityId;
}
