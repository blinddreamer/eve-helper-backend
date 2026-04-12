package com.example.pandatribe.models.industry.blueprints;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Embeddable
@Data
public class InvTypeMaterialKey implements Serializable {

    @Column(name = "\"typeID\"")
    private Integer typeId;

    @Column(name = "\"materialTypeID\"")
    private Integer materialTypeId;
}
