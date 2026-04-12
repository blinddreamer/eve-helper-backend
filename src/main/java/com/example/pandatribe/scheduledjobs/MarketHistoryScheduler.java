package com.example.pandatribe.scheduledjobs;

import com.example.pandatribe.models.dbmodels.market.MarketHistoryId;
import com.example.pandatribe.repositories.interfaces.MarketHistoryRepository;
import com.example.pandatribe.services.contracts.MarketService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class MarketHistoryScheduler {

    private static final String RUN_DAILY_AT_3_AM = "0 0 3 * * *";

    private final MarketHistoryRepository marketHistoryRepository;
    private final MarketService marketService;

    @Scheduled(cron = RUN_DAILY_AT_3_AM, zone = "UTC")
    public void appendDailyHistory() {
        List<MarketHistoryId> pairs = marketHistoryRepository.findDistinctTypeIdAndRegionId();
        log.info("Daily history update: processing {} type/region pairs", pairs.size());
        int updated = 0;
        for (MarketHistoryId pair : pairs) {
            try {
                marketService.getMarketHistory(pair.getTypeId(), pair.getRegionId());
                updated++;
            } catch (Exception e) {
                log.warn("Failed to update history for typeId={} regionId={}: {}",
                        pair.getTypeId(), pair.getRegionId(), e.getMessage());
            }
        }
        log.info("Daily history update complete: {}/{} pairs updated", updated, pairs.size());
    }
}
