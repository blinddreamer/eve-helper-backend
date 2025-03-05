package com.example.pandatribe.repositories.interfaces;

import com.example.pandatribe.models.industry.blueprints.EveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EveTypesRepository extends JpaRepository<EveType, Integer> {
    Optional<EveType> findEveTypeByTypeId(int typeId);
    List<EveType> findEveTypeByTypeName(String typeName);
}
