package com.example.pandatribe.models.industry.blueprints;

import jakarta.persistence.*;
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
