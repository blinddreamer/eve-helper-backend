package com.example.pandatribe.controllers;


import com.example.pandatribe.logging.JsonLogger;
import com.example.pandatribe.models.BlueprintData;
import com.example.pandatribe.models.requests.BlueprintRequest;
import com.example.pandatribe.models.results.GetBlueprintsResult;
import com.example.pandatribe.models.results.SystemName;
import com.example.pandatribe.models.universe.Region;
import com.example.pandatribe.models.universe.Station;
import com.example.pandatribe.services.contracts.BlueprintService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/")
public class EveBlueprintController {
    private static final Logger LOGGER = LoggerFactory.getLogger(EveBlueprintController.class);
    private final JsonLogger jsonLogger;
    private final BlueprintService blueprintService;

    @PostMapping("type")
    public ResponseEntity<?> getEveType(@RequestBody BlueprintRequest blueprintRequest){
        LOGGER.info("REQUEST for blueprint: ");
        jsonLogger.println(blueprintRequest);
        BlueprintData blueprintDto = blueprintService.getInitialBlueprintData(blueprintRequest);
        if(Objects.isNull(blueprintDto)){
            LOGGER.info("Blueprint {} not found", blueprintRequest.getBlueprintName());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Blueprint not found");
        }
        jsonLogger.println(blueprintDto);
        return ResponseEntity.ok(blueprintDto);
    }

    @PostMapping("update-type")
    public ResponseEntity<?> updateSubMaterials(@RequestBody BlueprintRequest subMaterialsRequest){
        LOGGER.info("REQUEST for submaterials: ");
        jsonLogger.println(subMaterialsRequest);
        BlueprintData blueprintDto = blueprintService.updateSubMaterials(subMaterialsRequest);
        if(Objects.isNull(blueprintDto)){
            LOGGER.info("Submaterials request {} not found", subMaterialsRequest.getRequestId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Request not found");
        }
        LOGGER.info("Submaterials request {} updated", subMaterialsRequest.getRequestId());
        jsonLogger.println(blueprintDto);
        return ResponseEntity.ok(blueprintDto);
    }

    @PostMapping("mass-update-type")
    public ResponseEntity<?> massUpdateSubMaterials(@RequestBody List<BlueprintRequest> subMaterialsRequest){
        LOGGER.info("REQUEST for submaterials: ");
        if(Objects.isNull(subMaterialsRequest) || subMaterialsRequest.isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request cannot be empty");
        }
        jsonLogger.println(subMaterialsRequest);
        BlueprintData blueprintDto = blueprintService.massUpdateMaterials(subMaterialsRequest);
        if(Objects.isNull(blueprintDto)){
            LOGGER.info("Submaterials request {} not found", subMaterialsRequest.get(0).getRequestId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Request not found");
        }
        LOGGER.info("Submaterials request {} updated", subMaterialsRequest.get(0).getRequestId());
        jsonLogger.println(blueprintDto);
        return ResponseEntity.ok(blueprintDto);
    }

    @GetMapping("systems")
    public ResponseEntity<List<SystemName>> getEveSystems(){
        LOGGER.debug("REQUEST for systems received");
        return ResponseEntity.ok(blueprintService.getEveSystems());
    }

    @GetMapping("blueprints")
    public ResponseEntity<GetBlueprintsResult> getEveBlueprints(){
        LOGGER.debug("REQUEST for blueprints received");
        return ResponseEntity.ok(blueprintService.getEveBlueprints());
    }

    @GetMapping("regions")
    public ResponseEntity<List<Region>> getEveRegions(){
        LOGGER.debug("REQUEST for regions received.");
        return ResponseEntity.ok(blueprintService.getEveRegions());
    }

    @GetMapping("stations")
    public ResponseEntity<List<Station>> getEveStations(){
        LOGGER.debug("REQUEST for stations received.");
        return ResponseEntity.ok(blueprintService.getEveStations());
    }
}
