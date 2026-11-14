package com.example.pandatribe.configs;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for EVE Online static data
 */
@Configuration
@Getter
public class EveDataConfig {

    /**
     * List of featured station IDs for quick access.
     * These are major trade hubs in EVE Online.
     */
    @Value("${eve.featured.stations:60004588,60008494,60011866,60005686,60003760}")
    private List<Long> featuredStationIds;

    /**
     * Default region ID (The Forge - Jita region)
     */
    @Value("${eve.default.region.id:10000002}")
    private Integer defaultRegionId;

    /**
     * Default system name
     */
    @Value("${eve.default.system:Jita}")
    private String defaultSystem;

    /**
     * Default location/station ID (Jita IV - Moon 4 - Caldari Navy Assembly Plant)
     */
    @Value("${eve.default.location.id:60003760}")
    private Long defaultLocationId;

    /**
     * Region from a "regionId_locationId" market key (e.g. "10000002_60003760").
     * Falls back to the default for null or malformed input instead of throwing.
     */
    public Integer resolveRegionId(String market) {
        String part = marketPart(market, 0);
        return part != null ? Integer.valueOf(part) : defaultRegionId;
    }

    /** Station/structure from a "regionId_locationId" market key; default when missing or malformed. */
    public Long resolveLocationId(String market) {
        String part = marketPart(market, 1);
        return part != null ? Long.valueOf(part) : defaultLocationId;
    }

    private static String marketPart(String market, int index) {
        // digit limits keep Integer/Long parsing from overflowing
        if (market == null || !market.matches("\\d{1,9}_\\d{1,18}")) {
            return null;
        }
        return market.split("_")[index];
    }
}
