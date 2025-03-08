package com.example.pandatribe.repositories.interfaces;

import com.example.pandatribe.models.market.Category;
import com.example.pandatribe.models.results.Blueprint;
import com.example.pandatribe.models.results.SystemName;
import com.example.pandatribe.models.industry.blueprints.BlueprintActivity;
import com.example.pandatribe.models.universe.Region;
import com.example.pandatribe.models.universe.Station;
import com.example.pandatribe.models.universe.SystemInfo;

import java.util.List;

public interface EveCustomRepository {
    BlueprintActivity getBluePrintInfoByProduct(Integer productId);
    BlueprintActivity getBluePrintInfoByBlueprint(Integer blueprintId);
    SystemInfo getSystemInfo(String systemName);
    Integer getVolume(Integer typeId);
    List<SystemName> getSystems();
    List<Blueprint> getBlueprints();
    List<Region> getRegions();
    List<Station> getStations();
    List<Category> getCategories();
    String getRegion(Integer regionId);
}
