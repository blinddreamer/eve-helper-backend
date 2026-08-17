package com.example.pandatribe.configs;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CacheConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfig.class);

    @CacheEvict(cacheNames = "cacheCalculator", allEntries = true)
    public void evictCacheCalculator(){
        LOGGER.info("Cache cacheCalculator was cleared");
    }

    @CacheEvict(cacheNames = "cacheMarketPrices", allEntries = true)
    public void evictCacheMarketPrices(){
        LOGGER.info("Cache cacheMarketPrices was cleared");
    }

    @CacheEvict(cacheNames = "cacheCostIndexes",allEntries = true)
    public void evictCacheCostIndexes(){LOGGER.info("Cache cacheCostIndexes was cleared");}

    @CacheEvict(cacheNames = "cacheItemMarketPrice",allEntries = true)
    public void evictCacheItemMarketPrice(){LOGGER.info("Cache cacheItemMarketPrice was cleared");}

    /**
     * Manual cache eviction methods for static data.
     * These can be called manually when needed (e.g., after SDE update).
     */
    @CacheEvict(cacheNames = {"blueprints", "regions", "stations", "systemNames", "marketGroups"}, allEntries = true, cacheManager = "staticDataCacheManager")
    public void evictAllStaticDataCaches(){
        LOGGER.info("All static data caches (blueprints, regions, stations, systemNames, marketGroups) were cleared");
    }

    @Scheduled(fixedRate = 900_000) // 15 minutes = 900,000 milliseconds
    public void triggerEvictCacheCalculator(){
        evictCacheCalculator();
        evictCacheItemMarketPrice();
    }

    @Scheduled(cron = "0 0 * * * *") // every hour
    public void triggerEvictCacheHourly(){
        evictCacheCostIndexes();
    }

    @CacheEvict(cacheNames = "cacheMarketTypes", cacheManager = "marketTypesCacheManager", allEntries = true)
    public void evictCacheMarketTypes() {
        LOGGER.info("Cache cacheMarketTypes was cleared");
    }

    @Scheduled(cron = "0 0 4 * * *") // every day at 4am
    public void triggerEvictCacheDaily(){
        evictCacheMarketPrices();
        evictCacheMarketTypes();
    }

}
