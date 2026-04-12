package com.example.pandatribe.repositories.interfaces;

import com.example.pandatribe.models.industry.blueprints.InvTypeMaterial;
import com.example.pandatribe.models.industry.blueprints.InvTypeMaterialKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvTypeMaterialRepository extends JpaRepository<InvTypeMaterial, InvTypeMaterialKey> {
    List<InvTypeMaterial> findByIdTypeId(Integer typeId);
}
