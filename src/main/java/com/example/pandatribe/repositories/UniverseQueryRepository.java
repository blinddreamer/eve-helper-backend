package com.example.pandatribe.repositories;

import com.example.pandatribe.configs.EveDataConfig;
import com.example.pandatribe.models.results.SystemName;
import com.example.pandatribe.models.universe.Region;
import com.example.pandatribe.models.universe.Station;
import com.example.pandatribe.models.universe.SystemInfo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository for EVE Universe queries.
 * Handles systems, regions, and stations.
 */
@Repository
@RequiredArgsConstructor
public class UniverseQueryRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(UniverseQueryRepository.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final EveDataConfig eveDataConfig;

    /**
     * Retrieves system information by system name.
     *
     * @param systemName The name of the solar system
     * @return SystemInfo containing system ID and security status, or null if not found
     */
    @Transactional(readOnly = true)
    public SystemInfo getSystemInfo(String systemName) {
        String nativeQuery = "SELECT solarSystemID, security " +
                "FROM mapSolarSystems " +
                "WHERE solarSystemName = :systemName";

        List<Object[]> result = entityManager.createNativeQuery(nativeQuery)
                .setParameter("systemName", systemName)
                .getResultList();

        return result.isEmpty() ? null :
                SystemInfo.builder()
                        .systemId((Integer) result.get(0)[0])
                        .security((Double) result.get(0)[1])
                        .build();
    }

    /**
     * Retrieves all solar system names from the database.
     *
     * @return List of all system names
     */
    @Transactional(readOnly = true)
    public List<SystemName> getAllSystems() {
        String nativeQuery = "SELECT solarSystemName FROM mapSolarSystems";
        List<Object> result = entityManager.createNativeQuery(nativeQuery).getResultList();

        LOGGER.debug("Retrieved {} solar systems", result.size());

        return result.stream()
                .map(name -> SystemName.builder().systemName((String) name).build())
                .toList();
    }

    /**
     * Retrieves all regions, ordered by name.
     *
     * @return List of all regions in alphabetical order
     */
    @Transactional(readOnly = true)
    public List<Region> getAllRegions() {
        String nativeQuery = "SELECT regionID, regionName " +
                "FROM mapRegions " +
                "ORDER BY regionName ASC";

        List<Object[]> result = entityManager.createNativeQuery(nativeQuery).getResultList();

        LOGGER.debug("Retrieved {} regions", result.size());

        return result.stream()
                .map(region -> Region.builder()
                        .regionId((Integer) region[0])
                        .regionName((String) region[1])
                        .build())
                .toList();
    }

    /**
     * Retrieves region name by ID.
     *
     * @param regionId The region ID
     * @return Region name, or null if not found
     */
    @Transactional(readOnly = true)
    public String getRegionName(Integer regionId) {
        String nativeQuery = "SELECT regionName FROM mapRegions WHERE regionID = :regionId";
        List<Object> result = entityManager.createNativeQuery(nativeQuery)
                .setParameter("regionId", regionId)
                .getResultList();

        return result.isEmpty() ? null : result.get(0).toString();
    }

    /**
     * Retrieves featured stations (major trade hubs).
     * Station IDs are configurable via application.properties.
     *
     * @return List of featured stations with region information
     */
    @Transactional(readOnly = true)
    public List<Station> getFeaturedStations() {
        List<Long> featuredStations = eveDataConfig.getFeaturedStationIds();

        String nativeQuery = "SELECT stationID, stationName, regionID " +
                "FROM staStations " +
                "WHERE stationID IN :stationIds " +
                "ORDER BY stationName ASC";

        List<Tuple> result = entityManager.createNativeQuery(nativeQuery, Tuple.class)
                .setParameter("stationIds", featuredStations)
                .getResultList();

        LOGGER.debug("Retrieved {} featured stations", result.size());

        return result.stream()
                .map(row -> Station.builder()
                        .stationId((Long) row.get("stationID"))
                        .stationName((String) row.get("stationName"))
                        .regionId((Integer) row.get("regionID"))
                        .regionName(getRegionName((Integer) row.get("regionID")))
                        .build())
                .toList();
    }
}
