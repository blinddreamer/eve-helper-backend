package com.example.pandatribe.repositories;

import com.example.pandatribe.models.industry.blueprints.BlueprintActivity;
import com.example.pandatribe.models.market.Category;
import com.example.pandatribe.models.results.Blueprint;
import com.example.pandatribe.models.results.SystemName;
import com.example.pandatribe.models.universe.Region;
import com.example.pandatribe.models.universe.Station;
import com.example.pandatribe.models.universe.SystemInfo;
import com.example.pandatribe.repositories.interfaces.EveCustomRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Repository
public class EveCustomRepositoryImpl implements EveCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @Override
    public BlueprintActivity getBluePrintInfoByProduct(Integer productId) {
        String nativeQuery = "SELECT quantity,typeID,activityID FROM industryActivityProducts iap WHERE productTypeID=:productId ";
        List<Object[]> result = entityManager.createNativeQuery(nativeQuery).setParameter("productId", productId).getResultList();
        return result.isEmpty() ? null :
                BlueprintActivity.builder()
                        .blueprintId((Integer) result.get(0)[1])
                        .craftQuantity((Integer) result.get(0)[0])
                        .activityId((Integer) result.get(0)[2])
                        .build();
    }

    @Transactional
    public BlueprintActivity getBluePrintInfoByBlueprint(Integer blueprintId) {
        String nativeQuery = "SELECT quantity, typeID, activityID FROM industryActivityProducts  WHERE typeID =:blueprintId ";
        List<Object[]> result = entityManager.createNativeQuery(nativeQuery).setParameter("blueprintId", blueprintId).getResultList();
        return result.isEmpty() ? null :
                BlueprintActivity.builder()
                        .blueprintId((Integer) result.get(0)[1])
                        .craftQuantity((Integer) result.get(0)[0])
                        .activityId((Integer) result.get(0)[2])
                        .build();
    }

    @Transactional
    public SystemInfo getSystemInfo(String systemName) {
        String nativeQuery = "SELECT solarSystemID, security FROM mapSolarSystems  WHERE solarSystemName = :systemName";
        List<Object[]> result = entityManager.createNativeQuery(nativeQuery).setParameter("systemName", systemName).getResultList();
        return result.isEmpty() ? null : SystemInfo.builder().systemId((Integer) result.get(0)[0]).security((Double) result.get(0)[1]).build();
    }

    @Transactional
    public List<SystemName> getSystems() {
        String nativeQuery = "SELECT solarSystemName FROM mapSolarSystems";
        List<Object> result = entityManager.createNativeQuery(nativeQuery).getResultList();
        return result.stream().map(name -> SystemName.builder().systemName((String) name).build()).toList();
    }

    @Transactional
    public Integer getVolume(Integer typeId) {
        String nativeQuery = "SELECT volume FROM invVolumes WHERE typeID = :typeId";
        List<Object> result = entityManager.createNativeQuery(nativeQuery).setParameter("typeId", typeId).getResultList();
        return result.isEmpty() ? null : (Integer) result.get(0);
    }

    @Transactional
    public List<Blueprint> getBlueprints() {
        String nativeQuery = "SELECT typeID, productTypeID, activityID FROM industryActivityProducts WHERE activityID = 1 OR activityID = 11";
        List<Tuple> result = entityManager.createNativeQuery(nativeQuery, Tuple.class).getResultList();
        return result.stream().map(row -> Blueprint.builder().bpId((Integer) row.get("typeID"))
                        .blueprint(getBlueprintName((Integer) row.get("productTypeID")))
                        .activity((Integer) row.get("activityID"))
                        .build())
                .filter(name -> Objects.nonNull(name.getBlueprint()))
                .filter(name -> !name.getBlueprint().contains("Edition")).toList();
    }

    @Transactional
    @Override
    public List<Region> getRegions() {
        String nativeQuery = "SELECT regionID, regionName FROM mapRegions ORDER BY regionName ASC";
        List<Object[]> result = entityManager.createNativeQuery(nativeQuery).getResultList();
        return result.stream().map(region -> Region.builder().regionId((Integer) region[0]).regionName((String) region[1]).build())
                .toList();
    }

    @Transactional
    @Override
    public List<Station> getStations() {
        String nativeQuery = "SELECT stationID, stationName, regionID FROM staStations WHERE stationID = 60004588 OR stationID = 60008494 OR stationID = 60011866 OR stationID = 60005686 OR stationID = 60003760 ORDER BY stationName ASC";
        List<Tuple> result = entityManager.createNativeQuery(nativeQuery, Tuple.class).getResultList();
        return result.stream().map(row -> Station.builder().stationId((Long) row.get("stationID"))
                        .stationName((String) row.get("stationName"))
                        .regionId((Integer) row.get("regionID"))
                        .regionName(getRegion((Integer) row.get("regionID")))
                        .build())
                .toList();
    }

    @Override
    public String getRegion(Integer regionId) {
        String nativeQuery = "SELECT regionName FROM mapRegions WHERE regionID = :regionId";
        List<Object> result = entityManager.createNativeQuery(nativeQuery).setParameter("regionId",regionId).getResultList();
        return result.isEmpty() ? null : result.get(0).toString();
    }

    @Override
    public List<Category> getCategories() {
        return new ArrayList<>();
    }


    private String getBlueprintName(Integer id) {
        String nativeQuery = "SELECT typeName FROM invTypes WHERE typeID = :id";
        List<Object> result = entityManager.createNativeQuery(nativeQuery).setParameter("id", id).getResultList();
        return result.isEmpty() ? null : (String) result.get(0);
    }
}
