package com.example.pandatribe.scheduledjobs;

import com.example.pandatribe.repositories.PlanetaryInteractionRepository;
import com.example.pandatribe.services.contracts.MarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.example.pandatribe.utils.Constant.DEFAULT_REGION_ID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PiMarketWarmupService {

    private final PlanetaryInteractionRepository planetaryInteractionRepository;
    private final MarketService marketService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmupOnStartup() {
        CompletableFuture.runAsync(() -> {
            log.info("PI market warmup starting on application startup...");
            refreshPiMarketOrders();
        });
    }

    // Runs every 15 min, first run delayed 15 min to avoid overlapping with startup warmup
    @Scheduled(fixedRate = 900_000, initialDelay = 900_000)
    public void scheduledRefresh() {
        log.info("PI market scheduled refresh starting...");
        refreshPiMarketOrders();
    }

    private void refreshPiMarketOrders() {
        List<Integer> piTypeIds;
        try {
            piTypeIds = planetaryInteractionRepository.getRawMaterials();
        } catch (Exception e) {
            log.error("Failed to load PI type IDs for market warmup", e);
            return;
        }

        if (piTypeIds.isEmpty()) {
            log.warn("No PI type IDs found — skipping market warmup");
            return;
        }

        log.info("Warming market orders for {} PI types in region {}", piTypeIds.size(), DEFAULT_REGION_ID);
        int success = 0;
        int failed = 0;

        for (Integer typeId : piTypeIds) {
            try {
                // getMarketOrders handles DB freshness check + ESI fallback automatically
                marketService.getMarketOrders(typeId, DEFAULT_REGION_ID);
                success++;
            } catch (Exception e) {
                log.warn("PI market warmup failed for typeId={}: {}", typeId, e.getMessage());
                failed++;
            }
        }

        log.info("PI market warmup complete: {}/{} refreshed, {} failed", success, piTypeIds.size(), failed);
    }
}
