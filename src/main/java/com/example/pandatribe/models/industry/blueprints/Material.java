package com.example.pandatribe.models.industry.blueprints;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
