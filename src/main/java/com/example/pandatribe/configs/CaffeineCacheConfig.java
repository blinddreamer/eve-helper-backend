package com.example.pandatribe.configs;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine cache configuration with different expiration policies for different cache types.
 *
 * Cache Types:
 * - Static SDE Data: blueprints, regions, stations, systemNames - NEVER expires (warmed on startup)
 * - Market Data: cacheItemMarketPrice, cacheMarketPrices, cacheCalculator, cacheBlueprintData - expire after 30 minutes
 * - Cost Indexes: cacheCostIndexes - expire after 1 hour
 *
 * Static data is pre-loaded on startup via CacheWarmingService for instant first requests.
 * Market data and cost indexes expire automatically via Caffeine's expireAfterWrite.
 */
@Configuration
public class CaffeineCacheConfig {

    /**
     * Primary cache manager for market and calculation data (short-lived).
     * Used for frequently changing data like market prices.
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "cacheItemMarketPrice",
            "cacheMarketPrices",
            "cacheCalculator",
            "cacheBlueprintData"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(30, TimeUnit.MINUTES));
        return cacheManager;
    }

    /**
     * Cache manager for static SDE data (never expires).
     * Used for blueprints, regions, stations, systems - data that only changes on SDE update.
     *
     * These caches are warmed on application startup and only cleared:
     * - On application restart
     * - After SDE update (via CacheConfig.evictAllStaticDataCaches())
     * - Manually via admin endpoint
     */
    @Bean("staticDataCacheManager")
    public CacheManager staticDataCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "blueprints",
            "regions",
            "stations",
            "systemNames"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(100));
            // No expireAfterWrite - cache never expires
        return cacheManager;
    }

    /**
     * Cache manager for cost indexes (medium-lived).
     * Updated hourly by ESI API.
     */
    @Bean("costIndexCacheManager")
    public CacheManager costIndexCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("cacheCostIndexes");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(1, TimeUnit.HOURS));
        return cacheManager;
    }

    /**
     * Cache manager for market type index (daily).
     * Holds the full list of tradeable item names for the market browser search.
     * Refreshed once per day via scheduled eviction.
     */
    @Bean("marketTypesCacheManager")
    public CacheManager marketTypesCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("cacheMarketTypes");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10)
            .expireAfterWrite(24, TimeUnit.HOURS));
        return cacheManager;
    }
}
