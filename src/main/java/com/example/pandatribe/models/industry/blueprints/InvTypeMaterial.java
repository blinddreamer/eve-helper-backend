package com.example.pandatribe.models.industry.blueprints;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "\"invTypeMaterials\"")
public class InvTypeMaterial {

    @EmbeddedId
    private InvTypeMaterialKey id;

    @Column(name = "\"quantity\"")
    private Integer quantity;
}
