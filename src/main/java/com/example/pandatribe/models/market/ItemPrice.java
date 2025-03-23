package com.example.pandatribe.models.market;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name="market_prices")
@Data
public class ItemPrice {
    @Id
    @Column(name = "id")
    private String id = UUID.randomUUID().toString();

    @JsonProperty(value = "duration")
    @Column(name= "duration")
    private Integer duration;

    @Column(name= "is_buy_order")
    @JsonProperty(value = "is_buy_order")
    private Boolean isBuyOrder;

    @Column(name = "issued")
    @JsonProperty(value = "issued")
    private Instant issued;

    @Column(name = "location_id")
    @JsonProperty(value = "location_id")
    private Long locationId;

    @Column(name = "min_volume")
    @JsonProperty(value = "min_volume")
    private Integer minVolume;

    @Column(name = "order_id")
    @JsonProperty(value = "order_id")
    private String orderId;

   @Column(name = "type_id")
    @JsonProperty(value = "type_id")
    private Integer typeId;

    @Column(name = "price")
    @JsonProperty(value = "price")
    private BigDecimal price;

    @Column(name = "system_id")
    @JsonProperty(value = "system_id")
    private String systemId;

    @Column(name = "range")
    @JsonProperty(value = "range")
    private String range;

    @Column(name = "volume_remain")
    @JsonProperty(value = "volume_remain")
    private Integer volumeRemain;

    @Column(name = "volume_total")
    @JsonProperty(value = "volume_total")
    private Integer volumeTotal;

    @Column(name = "extracted_on")
    private Instant extractedOn = Instant.now();

}
