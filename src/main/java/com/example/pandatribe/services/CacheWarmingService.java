package com.example.pandatribe.services;

import com.example.pandatribe.services.contracts.BlueprintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Service that warms up caches on application startup.
 * Pre-loads static SDE data (blueprints, regions, stations, systems) into cache
 * so first user requests are instant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheWarmingService {

    private final BlueprintService blueprintService;

    /**
     * Warms up static data caches after application is fully started.
     * Runs asynchronously to not block application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmupCaches() {
        log.info("Starting cache warmup for static SDE data...");

        long startTime = System.currentTimeMillis();

        try {
            // Warm up blueprints cache (most expensive)
            log.debug("Warming up blueprints cache...");
            blueprintService.getEveBlueprints();
            log.debug("Blueprints cache warmed up");

            // Warm up regions cache
            log.debug("Warming up regions cache...");
            blueprintService.getEveRegions();
            log.debug("Regions cache warmed up");

            // Warm up stations cache
            log.debug("Warming up stations cache...");
            blueprintService.getEveStations();
            log.debug("Stations cache warmed up");

            // Warm up systems cache
            log.debug("Warming up systems cache...");
            blueprintService.getEveSystems();
            log.debug("Systems cache warmed up");

            long duration = System.currentTimeMillis() - startTime;
            log.info("Cache warmup completed successfully in {}ms (blueprints, regions, stations, systems)", duration);

        } catch (Exception e) {
            log.error("Error during cache warmup - caches will be lazy-loaded on first request", e);
            // Don't throw exception - let application start even if cache warmup fails
        }
    }
}
