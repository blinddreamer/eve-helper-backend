package com.example.pandatribe.models.dbmodels.market;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_orders")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketOrderEntity {

    @Id
    @Column(name = "order_id")
    @JsonProperty("order_id")
    private Long orderId;

    @Column(name = "type_id")
    @JsonProperty("type_id")
    private Integer typeId;

    @Column(name = "region_id")
    @JsonProperty("region_id")
    private Integer regionId;

    @Column(name = "is_buy_order")
    @JsonProperty("is_buy_order")
    private Boolean isBuyOrder;

    @Column(name = "price")
    @JsonProperty("price")
    private BigDecimal price;

    @Column(name = "volume_remain")
    @JsonProperty("volume_remain")
    private Integer volumeRemain;

    @Column(name = "volume_total")
    @JsonProperty("volume_total")
    private Integer volumeTotal;

    @Column(name = "min_volume")
    @JsonProperty("min_volume")
    private Integer minVolume;

    @Column(name = "issued")
    @JsonProperty("issued")
    private Instant issued;

    @Column(name = "duration")
    @JsonProperty("duration")
    private Integer duration;

    @Column(name = "location_id")
    @JsonProperty("location_id")
    private Long locationId;

    @Column(name = "range")
    @JsonProperty("range")
    private String range;

    @Column(name = "fetched_at")
    @JsonIgnore
    private LocalDateTime fetchedAt;
}
