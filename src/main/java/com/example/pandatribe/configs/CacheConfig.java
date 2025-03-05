package com.example.pandatribe.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
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

    @Scheduled(fixedRate = 900_000) // 15 minutes = 900,000 milliseconds
    public void triggerEvictCacheCalculator(){
        evictCacheCalculator();
        evictCacheItemMarketPrice();
    }

    @Scheduled(cron = "0 0 * * * *") // every hour
    public void triggerEvictCache(){
        evictCacheMarketPrices();
        evictCacheCostIndexes();
    }

}
