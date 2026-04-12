package com.example.pandatribe.repositories.interfaces;

import com.example.pandatribe.models.dbmodels.market.MarketHistoryEntity;
import com.example.pandatribe.models.dbmodels.market.MarketHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketHistoryRepository extends JpaRepository<MarketHistoryEntity, MarketHistoryId> {

    List<MarketHistoryEntity> findByIdTypeIdAndIdRegionIdOrderByIdDateAsc(Integer typeId, Integer regionId);

    Optional<MarketHistoryEntity> findTopByIdTypeIdAndIdRegionIdOrderByIdDateDesc(Integer typeId, Integer regionId);

    @Query("SELECT DISTINCT new com.example.pandatribe.models.dbmodels.market.MarketHistoryId(h.id.typeId, h.id.regionId, null) " +
           "FROM MarketHistoryEntity h")
    List<MarketHistoryId> findDistinctTypeIdAndRegionId();
}
