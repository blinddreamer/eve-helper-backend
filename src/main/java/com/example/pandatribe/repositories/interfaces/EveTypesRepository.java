package com.example.pandatribe.repositories.interfaces;

import com.example.pandatribe.models.industry.blueprints.EveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface EveTypesRepository extends JpaRepository<EveType, Integer> {
    Optional<EveType> findEveTypeByTypeId(int typeId);
    List<EveType> findEveTypeByTypeName(String typeName);

    @Query("SELECT t.typeId FROM EveType t WHERE t.typeId IN :ids AND t.published = true AND t.marketGroupId IS NOT NULL")
    Set<Integer> findPublishedMarketTypeIds(@Param("ids") Collection<Integer> ids);

    List<EveType> findByMarketGroupIdAndPublishedTrueOrderByTypeNameAsc(Integer marketGroupId);
}
