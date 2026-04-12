package com.example.pandatribe.repositories.interfaces;

import com.example.pandatribe.models.dbmodels.market.MarketOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketOrderRepository extends JpaRepository<MarketOrderEntity, Long> {

    List<MarketOrderEntity> findByTypeIdAndRegionId(Integer typeId, Integer regionId);

    Optional<MarketOrderEntity> findTopByTypeIdAndRegionIdOrderByFetchedAtDesc(Integer typeId, Integer regionId);

    @Modifying
    @Transactional
    @Query("DELETE FROM MarketOrderEntity o WHERE o.typeId = :typeId AND o.regionId = :regionId")
    void deleteByTypeIdAndRegionId(@Param("typeId") Integer typeId, @Param("regionId") Integer regionId);

    @Modifying
    @Transactional
    @Query("DELETE FROM MarketOrderEntity o WHERE o.fetchedAt < :cutoff")
    void deleteStaleOrders(@Param("cutoff") LocalDateTime cutoff);
}
