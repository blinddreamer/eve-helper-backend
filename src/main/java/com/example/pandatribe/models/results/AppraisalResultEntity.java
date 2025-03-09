package com.example.pandatribe.models.results;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppraisalResultEntity {
    private String icon;
    private String item;
    private Double volume;
    private Integer quantity;
    private BigDecimal sellOrderPrice;
    private BigDecimal buyOrderPrice;
    private Long sellOrdersCount;
    private Long buyOrdersCount;
    private BigDecimal splitPrice;

}
