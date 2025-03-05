package com.example.pandatribe.models.market;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ItemPrice {
    @JsonProperty(value = "duration")
    private Integer duration;
    @JsonProperty(value = "is_buy_order")
    private Boolean isBuyOrder;
    @JsonProperty(value = "issued")
    private LocalDate issued;
    @JsonProperty(value = "location_id")
    private Long locationId;
    @JsonProperty(value = "min_volume")
    private Integer minVolume;
    @JsonProperty(value = "order_id")
    private String orderId;
    @JsonProperty(value = "type_id")
    private Integer typeId;
    @JsonProperty(value = "price")
    private BigDecimal price;
    @JsonProperty(value = "system_id")
    private String systemId;
    @JsonProperty(value = "range")
    private String range;
    @JsonProperty(value = "volume_remain")
    private Integer volumeRemain;
    @JsonProperty(value = "volume_total")
    private Integer volumeTotal;

}
