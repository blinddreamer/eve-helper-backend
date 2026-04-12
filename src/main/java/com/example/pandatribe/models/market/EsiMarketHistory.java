package com.example.pandatribe.models.market;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EsiMarketHistory {

    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("average")
    private BigDecimal average;

    @JsonProperty("highest")
    private BigDecimal highest;

    @JsonProperty("lowest")
    private BigDecimal lowest;

    @JsonProperty("volume")
    private Long volume;

    @JsonProperty("order_count")
    private Integer orderCount;
}
