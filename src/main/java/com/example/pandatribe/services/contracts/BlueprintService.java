package com.example.pandatribe.services.contracts;

import com.example.pandatribe.models.dbmodels.industry.BlueprintData;
import com.example.pandatribe.models.requests.BlueprintRequest;
import com.example.pandatribe.models.results.*;
import com.example.pandatribe.models.universe.Region;
import com.example.pandatribe.models.universe.Station;

import java.util.List;

public interface BlueprintService {
    BlueprintData getInitialBlueprintData(BlueprintRequest searchDto);
    BlueprintData massUpdateMaterials(List<BlueprintRequest> requests);
    BlueprintData updateSubMaterials(BlueprintRequest subMaterialsRequest);
    GetBlueprintsResult getEveBlueprints();
    List<SystemName> getEveSystems();
    List<Region> getEveRegions();
    List<Station> getEveStations();
}
