package com.example.pandatribe.services;

import com.example.pandatribe.models.BlueprintData;
import com.example.pandatribe.models.requests.BlueprintRequest;
import com.example.pandatribe.models.results.*;
import com.example.pandatribe.models.industry.CostIndex;
import com.example.pandatribe.models.industry.blueprints.BlueprintActivity;
import com.example.pandatribe.models.industry.blueprints.EveType;
import com.example.pandatribe.models.universe.Region;
import com.example.pandatribe.models.universe.Station;
import com.example.pandatribe.models.universe.SystemInfo;
import com.example.pandatribe.repositories.interfaces.EveCustomRepository;
import com.example.pandatribe.repositories.interfaces.EveTypesRepository;
import com.example.pandatribe.repositories.interfaces.BlueprintDataRepository;
import com.example.pandatribe.services.contracts.BlueprintService;
import com.example.pandatribe.services.contracts.IndustryService;
import com.example.pandatribe.services.contracts.MarketService;
import com.example.pandatribe.services.contracts.MaterialService;
import com.example.pandatribe.utils.Helper;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@AllArgsConstructor
public class BlueprintServiceImpl implements BlueprintService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintServiceImpl.class);
    public static final Integer REACTION_ACTIVITY_ID = 11;
    public static final Integer REGION_ID = 10000002;
    public static final String DEFAULT_SYSTEM = "Jita";
    public static final Integer DEFAULT_LOCATION_ID = 60003760;
    public static final String REACTION = "reaction";
    public static final String ORDER_TYPE = "sell";
    public static final String MANUFACTURING = "manufacturing";
    private final MaterialService materialsService;
    private final MarketService marketService;
    private final EveTypesRepository repository;
    private final EveCustomRepository eveCustomRepository;
    private final IndustryService industryService;
    private final Helper helper;
    private final BlueprintDataRepository blueprintDataRepository;


    @Override
    public BlueprintData getInitialBlueprintData(BlueprintRequest searchDto) {
        BlueprintResult blueprintData = getBlueprintData(searchDto);
        if (Objects.isNull(blueprintData)) {
            return null;
        }
        List<BlueprintResult> results = new ArrayList<>();
        results.add(blueprintData);
        results.addAll(blueprintData.getMaterialsList());
        return blueprintDataRepository.saveAndFlush(
                BlueprintData.builder().id(UUID.randomUUID().toString()).blueprintResult(results)
                        .creationDate(LocalDate.now()).build());
    }

    @Override
    public BlueprintData updateSubMaterials(BlueprintRequest subMaterialsRequest) {
        BlueprintData blueprintData = blueprintDataRepository.findById(subMaterialsRequest.getRequestId()).orElse(null);
        if (blueprintData == null) {
            return null;
        }
        BlueprintResult data = blueprintData.getBlueprintResult().stream().filter(req -> req.getName().equals(subMaterialsRequest.getBlueprintName()))
                .findFirst().orElse(null);


        BlueprintResult result = getBlueprintData(BlueprintRequest.builder()
                .blueprintMe(subMaterialsRequest.getBlueprintMe())
                .building(subMaterialsRequest.getBuilding())
                .buildingRig(subMaterialsRequest.getBuildingRig())
                .requestId(blueprintData.getId())
                .blueprintName(subMaterialsRequest.getBlueprintName())
                .tier(data.getTier())
                .runs(data.getQuantity())
                .count(subMaterialsRequest.getCount())
                .build());
        data.setMaterialsList(result.getMaterialsList());
        data.setIndustryCosts(result.getIndustryCosts());
        data.setSelectedForCraft(true);
        data.setAdjustedPrice(result.getAdjustedPrice());
        data.setCraftPrice(result.getCraftPrice());
        addSubmatirals(blueprintData.getBlueprintResult(), data.getMaterialsList(), subMaterialsRequest);
        blueprintData.getBlueprintResult().get(0).setCraftPrice(recalculateMasterCraftingPrice(blueprintData));
        blueprintDataRepository.saveAndFlush(blueprintData);
        return blueprintData;
    }

    @Override
    public GetBlueprintsResult getEveBlueprints() {
        List<Blueprint> blueprints = eveCustomRepository.getBlueprints().stream().filter(bp -> Objects.nonNull(bp.getBlueprint())).toList();
        LOGGER.info("Blueprints loaded - {}", !blueprints.isEmpty());
        return GetBlueprintsResult.builder()
                .blueprints(blueprints)
                .build();
    }

    @Override
    public List<SystemName> getEveSystems() {
        List<SystemName> systems = eveCustomRepository.getSystems();
        LOGGER.info("Systems loaded - {}", !systems.isEmpty());
        return systems;
    }

    @Override
    public List<Region> getEveRegions() {
        List<Region> regions = eveCustomRepository.getRegions();
        LOGGER.info("Regions loaded - {}", !regions.isEmpty());
        return regions;
    }

    @Override
    public List<Station> getEveStations() {
        List<Station> stations = eveCustomRepository.getStations();
        LOGGER.info("Stations loaded - {}", !stations.isEmpty());
        return stations;
    }

    private BigDecimal calculateIndustryTaxes(Double facilityPercent, Integer systemId, List<BlueprintResult> materials, String activity, Integer buildingIndex, Integer count) {
        BigDecimal eiv = materials.stream().map(BlueprintResult::getAdjustedPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        Integer buildingBonus = helper.getBuildingBonus(buildingIndex).getCostReduction();
        Double surcharge = 4.0;
        Double costIndex = industryService.getSystemCostIndexes().stream()
                .filter(c -> c.getSystemId().equals(systemId))
                .flatMap(systemIndex -> systemIndex.getCostIndexes().stream())
                .filter(c -> c.getActivity().equals(activity))
                .findFirst()
                .map(CostIndex::getCostIndex)
                .orElse(0.0);
        BigDecimal systemCost = eiv.multiply(BigDecimal.valueOf(costIndex));
        BigDecimal buildingCostReduction = BigDecimal.valueOf(buildingBonus).divide(BigDecimal.valueOf(100), RoundingMode.CEILING).multiply(systemCost);
        BigDecimal facilityTax = BigDecimal.valueOf(facilityPercent / 100).multiply(eiv).setScale(0, RoundingMode.CEILING);
        BigDecimal surChargeTax = BigDecimal.valueOf(surcharge / 100).multiply(eiv).setScale(0, RoundingMode.CEILING);
        BigDecimal finalPrice = (systemCost.subtract(buildingCostReduction)).add(facilityTax).add(surChargeTax);

        return finalPrice.setScale(0, RoundingMode.CEILING).multiply(BigDecimal.valueOf(count));
    }

    private BlueprintResult getBlueprintData(BlueprintRequest blueprintRequest) {
        Boolean init = Optional.ofNullable(blueprintRequest.getInit()).orElse(false);
        Integer runs = Optional.ofNullable(blueprintRequest.getRuns()).orElse(1);
        Integer blueprintMaterialEfficiency = Optional.ofNullable(blueprintRequest.getBlueprintMe()).orElse(0);
        Integer rigDiscount = Optional.ofNullable(blueprintRequest.getBuildingRig()).orElse(0);
        Integer buildingDiscount = Optional.ofNullable(blueprintRequest.getBuilding()).orElse(0);
        String system = Optional.ofNullable(blueprintRequest.getSystem()).filter(s -> !s.isEmpty()).orElse(DEFAULT_SYSTEM);
        Double facilityTax = Optional.ofNullable(blueprintRequest.getFacilityTax()).orElse(0.0);
        String blueprintName = blueprintRequest.getBlueprintName();
        Integer count = Optional.ofNullable(blueprintRequest.getCount()).orElse(1);
        Integer regionId = Optional.ofNullable(blueprintRequest.getRegionId()).orElse(REGION_ID);
        Integer tier = Optional.ofNullable(blueprintRequest.getTier()).orElse(0);
        Optional<EveType> eveType = repository.findEveTypeByTypeName(blueprintName);
        if (eveType.isEmpty()) {
            return null;
        }
        Integer size = init ? 256 : 32;
        BlueprintActivity blueprintActivity = eveCustomRepository.getBluePrintInfoByProduct(eveType.get().getTypeId());
        if (Objects.nonNull(blueprintActivity)) {
            SystemInfo systemInfo = eveCustomRepository.getSystemInfo(system);
            if (Objects.isNull(systemInfo)) {
                systemInfo = eveCustomRepository.getSystemInfo(DEFAULT_SYSTEM);
            }
            Integer volume = eveCustomRepository.getVolume(eveType.get().getTypeId());
            Integer matBlueprintId = blueprintActivity.getBlueprintId();
            Integer craftcount = (int) Math.ceil((double) runs / blueprintActivity.getCraftQuantity());
            Double craftQuantity = Optional.ofNullable(blueprintActivity).map(b -> Double.parseDouble(b.getCraftQuantity().toString())).orElse(1.0);
            List<BlueprintResult> materialsList = materialsService.getMaterialsByActivity(matBlueprintId, craftcount, rigDiscount, blueprintMaterialEfficiency, buildingDiscount, systemInfo.getSecurity(), count, regionId, tier);
            String activity = blueprintActivity.getActivityId().equals(REACTION_ACTIVITY_ID) ? REACTION : MANUFACTURING;
            BigDecimal industryCosts = calculateIndustryTaxes(facilityTax, systemInfo.getSystemId(), materialsList, activity, buildingDiscount, count);

            return BlueprintResult.builder()
                    .id(eveType.get().getTypeId())
                    .name(blueprintName)
                    .totalVolume((Objects.nonNull(volume) ? volume : eveType.get().getVolume()) * runs * count)
                    .volume((Objects.nonNull(volume) ? volume : eveType.get().getVolume()))
                    .isCreatable(Boolean.TRUE)
                    .quantity(runs * count)
                    .activityId(blueprintActivity.getActivityId())
                    .materialsList(materialsList)
                    .craftPrice(materialsList.stream().map(BlueprintResult::getTotalSellPrice).reduce(BigDecimal.ZERO, BigDecimal::add).add(industryCosts))
                    .industryCosts(industryCosts)
                    .excessMaterials( Math.abs(craftQuantity-(runs*count)))
                    .craftQuantity(craftQuantity)
                    .tier(tier)
                    .isFuel(blueprintName.contains("Fuel Block"))
                    .icon(eveType.get().getGroupId().equals(541) ? helper.generateRenderLink(eveType.get().getTypeId(), size) : helper.generateIconLink(eveType.get().getTypeId(), size))
                    .sellPrice(marketService
                            .getItemSellOrderPrice(DEFAULT_LOCATION_ID, marketService.getItemMarketPrice(eveType.get().getTypeId(), regionId, ORDER_TYPE)))
                    .totalSellPrice(marketService
                            .getItemSellOrderPrice(DEFAULT_LOCATION_ID, marketService.getItemMarketPrice(eveType.get().getTypeId(), regionId, ORDER_TYPE))
                            .multiply(BigDecimal.valueOf(runs))
                            .multiply(BigDecimal.valueOf(count)))
                    .build();

        }
        return null;
    }

    private void addSubmatirals(List<BlueprintResult> originalData, List<BlueprintResult> newMats, BlueprintRequest request) {
        newMats.forEach(mat -> {
            BlueprintResult existingMat = originalData.stream().filter(m -> m.getId().equals(mat.getId())).findFirst().orElse(null);
            if (Objects.nonNull(existingMat)) {
                existingMat.setQuantity(existingMat.getQuantity() + mat.getQuantity());
                if(Boolean.TRUE.equals(existingMat.getSelectedForCraft())){
                    request.setCount(existingMat.getQuantity());
                    existingMat = getBlueprintData(request);
                }

            } else {
                originalData.add(mat);
            }
        });
    }

    private void fixRecursivly(){

    }

    private void removeSubmaterials(BlueprintRequest blueprintRequest) {
    }

    private BigDecimal recalculateMasterCraftingPrice(BlueprintData blueprintData){
     return    blueprintData.getBlueprintResult().stream().filter(r-> r.getTier()>0)
                .map(r-> {
                    if(Boolean.TRUE.equals(r.getSelectedForCraft())){
                        return r.getIndustryCosts();
                    }
                    return r.getTotalSellPrice();
                }).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
