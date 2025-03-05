package com.example.pandatribe.repositories.interfaces;

import com.example.pandatribe.models.industry.blueprints.MaterialBlueprintKey;
import com.example.pandatribe.models.industry.blueprints.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EveMaterialsRepository extends JpaRepository<Material, MaterialBlueprintKey> {
    @Query(value = "SELECT * FROM industryActivityMaterials " +
            "WHERE typeID = ?1 AND (activityID = 1 or activityID = 11)", nativeQuery = true)
    List<Material> findMaterialsByActivity(Integer blueprintId);
}
