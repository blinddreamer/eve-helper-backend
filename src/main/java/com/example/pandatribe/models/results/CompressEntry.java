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
public class CompressEntry {
    private String originalName;
    private Long originalQuantity;
    private Double originalVolume;
    private Long remainder;

    private String compressedName;
    private Integer compressedTypeId;
    private String compressedIcon;
    private Long compressedQuantity;
    private Double compressedVolume;
    private Double volumeSaved;

    private BigDecimal originalSellPrice;
    private BigDecimal originalBuyPrice;
    private BigDecimal compressedSellPrice;
    private BigDecimal compressedBuyPrice;
}
