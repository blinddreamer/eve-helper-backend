package com.example.pandatribe.models.industry.blueprints;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "\"industryActivityMaterials\"")
public class Material {

    @EmbeddedId
    private MaterialBlueprintKey  blueprintTypeId;

    private Integer quantity;

    @Column(name = "\"activityID\"")
    private Integer activity;
}
