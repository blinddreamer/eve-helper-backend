package com.example.pandatribe.models.results;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReprocessResult {
    private Double efficiency;
    private List<ReprocessEntry> materials;
    private BigDecimal totalSell;
    private BigDecimal totalBuy;
    private BigDecimal totalSplit;
}
