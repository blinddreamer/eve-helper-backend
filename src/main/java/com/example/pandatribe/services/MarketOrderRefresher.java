package com.example.pandatribe.services;

import com.example.pandatribe.models.dbmodels.market.MarketOrderEntity;
import com.example.pandatribe.repositories.interfaces.MarketOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Isolated component for the delete+insert refresh cycle.
 * READ_COMMITTED disables InnoDB gap locks, preventing deadlocks between
 * concurrent inserts to adjacent (type_id, region_id) ranges.
 */
@Component
@RequiredArgsConstructor
public class MarketOrderRefresher {

    private final MarketOrderRepository marketOrderRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<MarketOrderEntity> refresh(Integer typeId, Integer regionId, List<MarketOrderEntity> entities) {
        marketOrderRepository.deleteByTypeIdAndRegionId(typeId, regionId);
        marketOrderRepository.saveAll(entities);
        return entities;
    }
}
