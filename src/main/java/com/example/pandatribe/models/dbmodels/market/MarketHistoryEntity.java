package com.example.pandatribe.models.dbmodels.market;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "market_history")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketHistoryEntity {

    @EmbeddedId
    private MarketHistoryId id;

    @Column(name = "average")
    @JsonProperty("average")
    private BigDecimal average;

    @Column(name = "highest")
    @JsonProperty("highest")
    private BigDecimal highest;

    @Column(name = "lowest")
    @JsonProperty("lowest")
    private BigDecimal lowest;

    @Column(name = "volume")
    @JsonProperty("volume")
    private Long volume;

    @Column(name = "order_count")
    @JsonProperty("order_count")
    private Integer orderCount;

    @JsonProperty("date")
    public String getDate() {
        return id != null ? id.getDate().toString() : null;
    }
}
