package com.example.pandatribe.services;

import com.example.pandatribe.feign.contracts.EveInteractor;
import com.example.pandatribe.models.dbmodels.market.MarketHistoryEntity;
import com.example.pandatribe.models.dbmodels.market.MarketHistoryId;
import com.example.pandatribe.models.dbmodels.market.MarketOrderEntity;
import com.example.pandatribe.models.market.EsiMarketHistory;
import com.example.pandatribe.models.market.ItemPrice;
import com.example.pandatribe.models.market.MarketPriceData;
import com.example.pandatribe.models.market.MarketType;
import com.example.pandatribe.repositories.interfaces.MarketHistoryRepository;
import com.example.pandatribe.repositories.interfaces.MarketOrderRepository;
import com.example.pandatribe.services.contracts.MarketService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class MarketServiceImpl implements MarketService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarketServiceImpl.class);
    private static final int ORDER_CACHE_MINUTES = 15;
    private final EveInteractor eveInteractor;
    private final MarketOrderRepository marketOrderRepository;
    private final MarketHistoryRepository marketHistoryRepository;
    private final MarketOrderRefresher marketOrderRefresher;
    // Not a Spring bean — excluded from @AllArgsConstructor via initializer
    private final ConcurrentHashMap<String, ReentrantLock> orderRefreshLocks = new ConcurrentHashMap<>();  //NOSONAR
    public static final String DATA_SOURCE = "tranquility";

    @Override
    @Cacheable(value = "cacheItemMarketPrice", key = "#typeId + '_' + #regionId + '_' + #orderType")
    public List<ItemPrice> getItemMarketPrice(Integer typeId, Integer regionId, String orderType) {
        List<ItemPrice> itemPriceList = eveInteractor.getItemMarketPrice(regionId, DATA_SOURCE, orderType, typeId);
        LOGGER.info("Item {} prices obtained {}", typeId, !itemPriceList.isEmpty());
        return itemPriceList;
    }

    @Override
    public BigDecimal getItemSellOrderPrice(Integer locationId, List<ItemPrice> itemPriceList) {
        return itemPriceList.stream()
                .map(ItemPrice::getPrice)
                .sorted()
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal getItemPriceByOrderType(String orderType, List<ItemPrice> itemPriceList, Long locationId) {
        if (orderType.equals("buy")) {
            return itemPriceList.stream()
                    .filter(itemPrice -> Objects.equals(itemPrice.getLocationId(), locationId))
                    .filter(itemPrice -> Objects.equals(itemPrice.getIsBuyOrder(), true))
                    .map(ItemPrice::getPrice).max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        }
        return itemPriceList.stream()
                .filter(itemPrice -> Objects.equals(itemPrice.getLocationId(), locationId))
                .filter(itemPrice -> Objects.equals(itemPrice.getIsBuyOrder(), false))
                .map(ItemPrice::getPrice)
                .sorted()
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal getItemPriceByOrderTypeFromOrders(String orderType, List<MarketOrderEntity> orders, Long locationId) {
        if (orderType.equals("buy")) {
            return orders.stream()
                    .filter(o -> Objects.equals(o.getLocationId(), locationId))
                    .filter(o -> Boolean.TRUE.equals(o.getIsBuyOrder()))
                    .map(MarketOrderEntity::getPrice).max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        }
        return orders.stream()
                .filter(o -> Objects.equals(o.getLocationId(), locationId))
                .filter(o -> Boolean.FALSE.equals(o.getIsBuyOrder()))
                .map(MarketOrderEntity::getPrice)
                .sorted()
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Cacheable("cacheMarketPrices")
    public List<MarketPriceData> getMarketPriceData() {
        List<MarketPriceData> marketPriceDataList = eveInteractor.getMarketPrices();
        LOGGER.info("All market prices data is obtained: {}", !marketPriceDataList.isEmpty());
        return marketPriceDataList;
    }

    @Override
    @Cacheable(value = "cacheMarketTypes", cacheManager = "marketTypesCacheManager")
    public List<MarketType> getMarketTypes() {
        List<MarketType> types = eveInteractor.getMarketTypes();
        LOGGER.info("Market type index cached: {} types", types.size());
        return types;
    }

    @Override
    public List<MarketOrderEntity> getMarketOrders(Integer typeId, Integer regionId) {
        // Optimistic read — no lock needed if cache is fresh
        if (isFresh(marketOrderRepository.findTopByTypeIdAndRegionIdOrderByFetchedAtDesc(typeId, regionId))) {
            return marketOrderRepository.findByTypeIdAndRegionId(typeId, regionId);
        }

        // Acquire per-key lock so only one thread refreshes a given typeId+regionId at a time
        ReentrantLock lock = orderRefreshLocks.computeIfAbsent(typeId + "_" + regionId, k -> new ReentrantLock());
        lock.lock();
        try {
            // Double-check after acquiring lock — another thread may have just refreshed
            if (isFresh(marketOrderRepository.findTopByTypeIdAndRegionIdOrderByFetchedAtDesc(typeId, regionId))) {
                LOGGER.info("Returning DB-cached orders for typeId={} regionId={}", typeId, regionId);
                return marketOrderRepository.findByTypeIdAndRegionId(typeId, regionId);
            }

            LOGGER.info("Fetching orders from ESI for typeId={} regionId={}", typeId, regionId);
            List<ItemPrice> esiOrders = eveInteractor.getMarketOrders(typeId, regionId);
            LocalDateTime now = LocalDateTime.now();

            List<MarketOrderEntity> entities = esiOrders.stream()
                    .map(ip -> MarketOrderEntity.builder()
                            .orderId(Long.parseLong(ip.getOrderId()))
                            .typeId(ip.getTypeId())
                            .regionId(regionId)
                            .isBuyOrder(ip.getIsBuyOrder())
                            .price(ip.getPrice())
                            .volumeRemain(ip.getVolumeRemain())
                            .volumeTotal(ip.getVolumeTotal())
                            .minVolume(ip.getMinVolume())
                            .issued(ip.getIssued())
                            .duration(ip.getDuration())
                            .locationId(ip.getLocationId())
                            .range(ip.getRange())
                            .fetchedAt(now)
                            .build())
                    .collect(Collectors.toList());

            // Run delete+insert with READ_COMMITTED isolation to avoid InnoDB gap-lock deadlocks
            List<MarketOrderEntity> saved = marketOrderRefresher.refresh(typeId, regionId, entities);
            LOGGER.info("Cached {} orders for typeId={} regionId={}", saved.size(), typeId, regionId);
            return saved;
        } finally {
            lock.unlock();
        }
    }

    private boolean isFresh(Optional<MarketOrderEntity> latest) {
        return latest.isPresent()
                && latest.get().getFetchedAt().isAfter(LocalDateTime.now().minusMinutes(ORDER_CACHE_MINUTES));
    }

    @Override
    @Transactional
    public List<MarketHistoryEntity> getMarketHistory(Integer typeId, Integer regionId) {
        List<MarketHistoryEntity> dbHistory = marketHistoryRepository
                .findByIdTypeIdAndIdRegionIdOrderByIdDateAsc(typeId, regionId);

        if (!dbHistory.isEmpty()) {
            LocalDate latestDate = dbHistory.get(dbHistory.size() - 1).getId().getDate();
            if (!latestDate.isBefore(LocalDate.now().minusDays(1))) {
                LOGGER.info("Returning DB history for typeId={} regionId={} ({} entries)", typeId, regionId, dbHistory.size());
                return dbHistory;
            }
            return appendNewHistory(typeId, regionId, dbHistory, latestDate);
        }

        LOGGER.info("Backfilling history from ESI for typeId={} regionId={}", typeId, regionId);
        List<EsiMarketHistory> esiHistory = eveInteractor.getMarketHistory(regionId, typeId);

        List<MarketHistoryEntity> entities = esiHistory.stream()
                .map(h -> MarketHistoryEntity.builder()
                        .id(new MarketHistoryId(typeId, regionId, h.getDate()))
                        .average(h.getAverage())
                        .highest(h.getHighest())
                        .lowest(h.getLowest())
                        .volume(h.getVolume())
                        .orderCount(h.getOrderCount())
                        .build())
                .collect(Collectors.toList());

        marketHistoryRepository.saveAll(entities);
        LOGGER.info("Backfilled {} history entries for typeId={} regionId={}", entities.size(), typeId, regionId);
        return marketHistoryRepository.findByIdTypeIdAndIdRegionIdOrderByIdDateAsc(typeId, regionId);
    }

    List<MarketHistoryEntity> appendNewHistory(Integer typeId, Integer regionId,
                                               List<MarketHistoryEntity> existing, LocalDate latestDate) {
        List<EsiMarketHistory> esiHistory = eveInteractor.getMarketHistory(regionId, typeId);
        List<MarketHistoryEntity> newEntries = esiHistory.stream()
                .filter(h -> h.getDate().isAfter(latestDate))
                .map(h -> MarketHistoryEntity.builder()
                        .id(new MarketHistoryId(typeId, regionId, h.getDate()))
                        .average(h.getAverage())
                        .highest(h.getHighest())
                        .lowest(h.getLowest())
                        .volume(h.getVolume())
                        .orderCount(h.getOrderCount())
                        .build())
                .collect(Collectors.toList());

        if (!newEntries.isEmpty()) {
            marketHistoryRepository.saveAll(newEntries);
            LOGGER.info("Appended {} history entries for typeId={} regionId={}", newEntries.size(), typeId, regionId);
            return marketHistoryRepository.findByIdTypeIdAndIdRegionIdOrderByIdDateAsc(typeId, regionId);
        }
        return existing;
    }
}
