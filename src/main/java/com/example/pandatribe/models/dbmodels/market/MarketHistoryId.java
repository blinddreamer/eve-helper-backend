package com.example.pandatribe.models.dbmodels.market;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MarketHistoryId implements Serializable {

    @Column(name = "type_id")
    private Integer typeId;

    @Column(name = "region_id")
    private Integer regionId;

    @Column(name = "date")
    private LocalDate date;
}
