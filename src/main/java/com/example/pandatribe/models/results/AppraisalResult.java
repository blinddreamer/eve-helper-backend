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
public class AppraisalResult {
    List<AppraisalResultEntity> appraisals;
    BigDecimal estimateTotalBuy;
    BigDecimal estimateTotalSell;
    BigDecimal estimateTotalSplit;
    Double totalVolume;
    String regionId;
    // Pasted names that matched no EVE item, so the UI can tell the user instead of dropping them
    List<String> missingItems;
}
