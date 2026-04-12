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
public class ReprocessEntry {
    private Integer typeId;
    private String name;
    private String icon;
    private Long quantity;
    private BigDecimal sellPrice;
    private BigDecimal buyPrice;
    private BigDecimal totalSell;
    private BigDecimal totalBuy;
}
