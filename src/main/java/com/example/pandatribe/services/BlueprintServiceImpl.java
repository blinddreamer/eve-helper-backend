package com.example.pandatribe.services;

import com.example.pandatribe.models.BlueprintData;
import com.example.pandatribe.models.industry.CostIndex;
import com.example.pandatribe.models.industry.blueprints.BlueprintActivity;
import com.example.pandatribe.models.industry.blueprints.EveType;
import com.example.pandatribe.models.market.ItemPrice;
import com.example.pandatribe.models.requests.BlueprintRequest;
import com.example.pandatribe.models.requests.MaterialInfo;
import com.example.pandatribe.models.results.Blueprint;
import com.example.pandatribe.models.results.BlueprintResult;
import com.example.pandatribe.models.results.GetBlueprintsResult;
import com.example.pandatribe.models.results.SystemName;
import com.example.pandatribe.models.universe.Region;
import com.example.pandatribe.models.universe.Station;
import com.example.pandatribe.models.universe.SystemInfo;
import com.example.pandatribe.repositories.interfaces.BlueprintDataRepository;
import com.example.pandatribe.repositories.interfaces.EveCustomRepository;
import com.example.pandatribe.repositories.interfaces.EveTypesRepository;
import com.example.pandatribe.services.contracts.BlueprintService;
import com.example.pandatribe.services.contracts.IndustryService;
import com.example.pandatribe.services.contracts.MarketService;
import com.example.pandatribe.services.contracts.MaterialService;
import com.example.pandatribe.utils.Helper;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
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
    public static final String ORDER_TYPE_ALL = "all";
    public static final String ORDER_TYPE_BUY = "buy";
    public static final String ORDER_TYPE_SELL = "sell";
    public static final String MANUFACTURING = "manufacturing";
    private final MaterialService materialsService;
    private final MarketService marketService;
    private final EveTypesRepository repository;
    private final EveCustomRepository eveCustomRepository;
    private final IndustryService industryService;
    private final Helper helper;
    private final BlueprintDataRepository blueprintDataRepository;
    private final ApplicationContext applicationContext;

    @Override
    public BlueprintData getInitialBlueprintData(BlueprintRequest searchDto) {
        BlueprintServiceImpl self = applicationContext.getBean(BlueprintServiceImpl.class);
        BlueprintResult initialBlueprint = self.getBlueprintData(searchDto);
        if (Objects.isNull(initialBlueprint)) {
            return null;
        }
        return blueprintDataRepository.saveAndFlush(
                BlueprintData.builder().id(UUID.randomUUID().toString())
                        .blueprintResult(Collections.singletonList(initialBlueprint))
                        .creationDate(LocalDate.now()).build());
    }

    public BlueprintData massUpdateMaterials(List<BlueprintRequest> requests) {
        BlueprintData blueprintData = blueprintDataRepository.findById(requests.get(0).getRequestId()).orElse(null);
        if (blueprintData == null) {
            return null;
        }
        requests.forEach(request -> updateBlueprintData(blueprintData, request));
        return blueprintDataRepository.saveAndFlush(blueprintData);
    }

    @Override
    public BlueprintData updateSubMaterials(BlueprintRequest subMaterialsRequest) {
        BlueprintData blueprintData = blueprintDataRepository.findById(subMaterialsRequest.getRequestId()).orElse(null);
        if (blueprintData == null) {
            return null;
        }
        return blueprintDataRepository.saveAndFlush(updateBlueprintData(blueprintData, subMaterialsRequest));
    }

    @Override
    public GetBlueprintsResult getEveBlueprints() {
        List<Blueprint> blueprints = eveCustomRepository.getBlueprints();
        LOGGER.info("Blueprints loaded - {}", !blueprints.isEmpty());

        return GetBlueprintsResult.builder()
                .blueprints(blueprints.stream().map(bp ->
                        bp.withComplexity(materialsService.getBlueprintComplexity(bp.getBpId()))).toList())
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

    private BigDecimal calculateIndustryTaxes(Double facilityPercent, Integer systemId, List<MaterialInfo> materials, String activity, Integer buildingIndex, Integer count) {
        BigDecimal eiv = materials.stream().map(MaterialInfo::getAdjustedPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
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
        BigDecimal buildingCostReduction = BigDecimal.valueOf(buildingBonus).divide(BigDecimal.valueOf(100)).multiply(systemCost);
        BigDecimal facilityTax = BigDecimal.valueOf(facilityPercent / 100).multiply(eiv).setScale(0, RoundingMode.CEILING);
        BigDecimal surChargeTax = BigDecimal.valueOf(surcharge / 100).multiply(eiv).setScale(0, RoundingMode.CEILING);
        BigDecimal finalPrice = (systemCost.subtract(buildingCostReduction)).add(facilityTax).add(surChargeTax);

        return finalPrice.setScale(0, RoundingMode.CEILING).multiply(BigDecimal.valueOf(count));
    }

    @Cacheable(value = "cacheCalculator")
    public BlueprintResult getBlueprintData(BlueprintRequest blueprintRequest) {
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
        EveType eveType = repository.findEveTypeByTypeName(blueprintName).stream().findFirst().orElse(null);
        if (Objects.isNull(eveType)) {
            return null;
        }
        Integer size = Boolean.TRUE.equals(init) ? 256 : 32;
        BlueprintActivity blueprintActivity = eveCustomRepository.getBluePrintInfoByProduct(eveType.getTypeId());
        if (Objects.nonNull(blueprintActivity)) {
            SystemInfo systemInfo = eveCustomRepository.getSystemInfo(system);
            if (Objects.isNull(systemInfo)) {
                systemInfo = eveCustomRepository.getSystemInfo(DEFAULT_SYSTEM);
            }
            Integer volume = eveCustomRepository.getVolume(eveType.getTypeId());
            Integer matBlueprintId = blueprintActivity.getBlueprintId();
            Integer craftCount = (int) Math.ceil((double) runs / blueprintActivity.getCraftQuantity());
            Double craftQuantity = Optional.of(blueprintActivity).map(b -> Double.parseDouble(b.getCraftQuantity().toString())).orElse(1.0);
            List<MaterialInfo> materialsList = materialsService.getMaterialsByActivity(matBlueprintId, craftCount, rigDiscount, blueprintMaterialEfficiency, buildingDiscount, systemInfo.getSecurity(), count, regionId, tier);
            String activity = blueprintActivity.getActivityId().equals(REACTION_ACTIVITY_ID) ? REACTION : MANUFACTURING;
            BigDecimal industryCosts = calculateIndustryTaxes(facilityTax, systemInfo.getSystemId(), materialsList, activity, buildingDiscount, count);
            List<ItemPrice> itemPriceList = marketService.getItemMarketPrice(eveType.getTypeId(), regionId, ORDER_TYPE_ALL);
            BigDecimal buyPrice = marketService
                    .getItemPriceByOrderType(ORDER_TYPE_BUY, itemPriceList);
            BigDecimal sellPrice = marketService
                    .getItemPriceByOrderType(ORDER_TYPE_SELL, itemPriceList);
            return BlueprintResult.builder()
                    .id(eveType.getTypeId())
                    .name(blueprintName)
                    .totalVolume((Objects.nonNull(volume) ? volume : eveType.getVolume()) * runs * count)
                    .volume((Objects.nonNull(volume) ? volume : eveType.getVolume()))
                    .isCreatable(Boolean.TRUE)
                    .quantity(runs * count)
                    .activityId(blueprintActivity.getActivityId())
                    .materialsList(materialsList)
                    .buyCraftPrice(materialsList.stream().map(materialInfo -> materialInfo.getBuyPrice().multiply(BigDecimal.valueOf(materialInfo.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add).add(industryCosts))
                    .sellCraftPrice((materialsList.stream().map(materialInfo -> materialInfo.getSellPrice().multiply(BigDecimal.valueOf(materialInfo.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add).add(industryCosts)))
                    .industryCosts(industryCosts)
                    // .excessMaterials(Math.abs(craftQuantity >1 ? craftQuantity -(runs * count) : 0 ))
                    .craftQuantity(craftQuantity)
                    .tier(tier)
                    .isFuel(blueprintName.contains("Fuel Block"))
                    .blueprintMaterialEfficiency(blueprintMaterialEfficiency)
                    .facilityTax(facilityTax)
                    .regionId(regionId)
                    .system(system)
                    .buildingDiscount(buildingDiscount)
                    .selectedForCraft(Boolean.TRUE)
                    .rigDiscount(rigDiscount)
                    .icon(eveType.getGroupId().equals(541) ? helper.generateRenderLink(eveType.getTypeId(), size) : helper.generateIconLink(eveType.getTypeId(), size))
                    .buyPrice(buyPrice)
                    .totalBuyPrice(buyPrice.multiply(BigDecimal.valueOf(runs)).multiply(BigDecimal.valueOf(count)))
                    .sellPrice(sellPrice)
                    .totalSellPrice(sellPrice.multiply(BigDecimal.valueOf(runs)).multiply(BigDecimal.valueOf(count)))
                    .jobsCount(craftCount)
                    .build();
        }
        return null;
    }

    private BlueprintData updateBlueprintData(BlueprintData blueprintData, BlueprintRequest subMaterialsRequest) {
        Map<String, Integer> initialQuantities = new HashMap<>();
        List<BlueprintResult> originalData = blueprintData.getBlueprintResult();
        blueprintData.getBlueprintResult().forEach(result -> {
            //Integer quant = calculateQuantity(originalData, result.getName());
            initialQuantities.put(result.getName(), result.getQuantity());
        });
        BlueprintResult alreadyExistingData = blueprintData.getBlueprintResult().stream().filter(mat -> mat.getName().equals(subMaterialsRequest.getBlueprintName())).findFirst().orElse(null);
        if (Objects.nonNull(alreadyExistingData)) {
            List<BlueprintResult> tempList = new ArrayList<>();
            alreadyExistingData.setSelectedForCraft(!alreadyExistingData.getSelectedForCraft());
            if (Boolean.FALSE.equals(alreadyExistingData.getSelectedForCraft())) {
                adjustSelectedItems(originalData, alreadyExistingData);
            }
            tempList.add(originalData.get(0));
            originalData.stream().skip(1).forEach(mat -> tempList.add(updateNeededMaterials(originalData, mat, initialQuantities)));
            blueprintData = blueprintData.withBlueprintResult(tempList);
            BlueprintResult initialBlueprint = tempList.get(0);
            BigDecimal buyCraftPrice = recalculateMasterCraftingPrice(blueprintData, true).add(initialBlueprint.getIndustryCosts());
            BigDecimal sellCraftPrice = recalculateMasterCraftingPrice(blueprintData, false).add(initialBlueprint.getIndustryCosts());
            initialBlueprint.setBuyCraftPrice(buyCraftPrice);
            initialBlueprint.setSellCraftPrice(sellCraftPrice);
            return blueprintData.withBlueprintResult(tempList);
        } else {

            List<BlueprintResult> newData = updateList(blueprintData.getBlueprintResult(), subMaterialsRequest, initialQuantities);
//            newData.get(0).setCraftPrice(recalculateMasterCraftingPrice(blueprintData).add(newData.get(0).getIndustryCosts()));
            BlueprintResult initialBlueprint = newData.get(0);
            BigDecimal buyCraftPrice = recalculateMasterCraftingPrice(blueprintData, true).add(initialBlueprint.getIndustryCosts());
            BigDecimal sellCraftPrice = recalculateMasterCraftingPrice(blueprintData, false).add(initialBlueprint.getIndustryCosts());
            initialBlueprint.setBuyCraftPrice(buyCraftPrice);
            initialBlueprint.setSellCraftPrice(sellCraftPrice);
            return blueprintData.withBlueprintResult(newData);
        }
    }

    private BigDecimal recalculateMasterCraftingPrice(BlueprintData blueprintData, Boolean isBuyPrice) {

        List<MaterialInfo> initialMatList = blueprintData.getBlueprintResult().get(0).getMaterialsList();
        List<BlueprintResult> selectedForCraftList = blueprintData.getBlueprintResult();
        return initialMatList.stream().map(mat -> {
                    BlueprintResult existingMat = selectedForCraftList.stream().filter(bp -> bp.getName().equals(mat.getName())).findFirst().orElse(null);
                    if (Objects.nonNull(existingMat)) {
                        if (Boolean.TRUE.equals(existingMat.getSelectedForCraft())) {
                            return recalculateSubMaterialsCraftingPrices(existingMat.getMaterialsList(), selectedForCraftList, isBuyPrice).add(existingMat.getIndustryCosts());
                        } else {
                            return mat.getBuyPrice().multiply(BigDecimal.valueOf(mat.getQuantity()));
                        }
                    } else {
                        return mat.getBuyPrice().multiply(BigDecimal.valueOf(calculateQuantity(selectedForCraftList, mat.getName())));
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal recalculateSubMaterialsCraftingPrices(List<MaterialInfo> materialsList, List<BlueprintResult> selectedForCraftList, Boolean isBuyPrice) {
        return materialsList.stream().map(mat -> {
                    BlueprintResult existingMat = selectedForCraftList.stream().filter(bp -> bp.getName().equals(mat.getName())).findFirst().orElse(null);
                    if (Objects.nonNull(existingMat)) {
                        if (Boolean.TRUE.equals(existingMat.getSelectedForCraft())) {
                            return recalculateSubMaterialsCraftingPrices(existingMat.getMaterialsList(), selectedForCraftList, isBuyPrice).add(existingMat.getIndustryCosts());
                        } else {
                            return mat.getBuyPrice().multiply(BigDecimal.valueOf(mat.getQuantity()));
                        }
                    } else {
                        return mat.getBuyPrice().multiply(BigDecimal.valueOf(mat.getQuantity()));
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<BlueprintResult> updateList(List<BlueprintResult> blueprintDataResult, BlueprintRequest subMaterialsRequest,
                                             Map<String, Integer> initialQuantities) {
        List<BlueprintResult> tempList = new ArrayList<>();
        BlueprintServiceImpl self = applicationContext.getBean(BlueprintServiceImpl.class);
        Integer quant = calculateQuantity(blueprintDataResult, subMaterialsRequest.getBlueprintName());
        BlueprintResult result = self.getBlueprintData(BlueprintRequest.builder()
                .blueprintName(subMaterialsRequest.getBlueprintName())
                .runs(quant)
                .blueprintMe(subMaterialsRequest.getBlueprintMe())
                .system(subMaterialsRequest.getSystem())
                .tier(subMaterialsRequest.getTier())
                .regionId(subMaterialsRequest.getRegionId())
                .facilityTax(subMaterialsRequest.getFacilityTax())
                .buildingRig(subMaterialsRequest.getBuildingRig())
                .building(subMaterialsRequest.getBuilding())
                .build());
        blueprintDataResult.add(result);
        tempList.add(blueprintDataResult.get(0));
        blueprintDataResult.stream().skip(1).forEach(mat -> tempList.add(updateNeededMaterials(blueprintDataResult, mat, initialQuantities)));
        return tempList;
    }

    private void adjustSelectedItems(List<BlueprintResult> originalData, BlueprintResult selectedItem) {
        selectedItem.getMaterialsList().forEach(material -> {
            BlueprintResult alreadyExist = originalData.stream().filter(mat -> mat.getName().equals(material.getName())).findFirst().orElse(null);
            if (Objects.nonNull(alreadyExist)) {
                alreadyExist.setSelectedForCraft(Boolean.FALSE);
                adjustSelectedItems(originalData, alreadyExist);
            }
        });
    }

    private Integer calculateQuantity(List<BlueprintResult> originalData, String blueprintName) {
        return originalData.stream().filter(mat -> mat.getSelectedForCraft() && mat.getMaterialsList().stream().anyMatch(m -> m.getName().equals(blueprintName)))
                .flatMap(mat -> mat.getMaterialsList().stream()).filter(m -> m.getName().equals(blueprintName)).map(MaterialInfo::getQuantity).reduce(Integer::sum).orElse(0);
    }

    private BlueprintResult updateNeededMaterials(List<BlueprintResult> originalData, BlueprintResult material,
                                                  Map<String, Integer> initialQuantities) {
        BlueprintServiceImpl self = applicationContext.getBean(BlueprintServiceImpl.class);
        Integer quant = calculateQuantity(originalData, material.getName());

//        if (initialQuantities.containsKey(material.getName()) && !Objects.equals(initialQuantities.get(material.getName()), quant)) {
        if (quant == 0 || Boolean.FALSE.equals(material.getSelectedForCraft())) {
            return material.withSelectedForCraft(Boolean.FALSE);
        }
        return self.getBlueprintData(BlueprintRequest.builder()
                .blueprintName(material.getName())
                .runs(quant)
                .blueprintMe(material.getBlueprintMaterialEfficiency())
                .system(material.getSystem())
                .regionId(material.getRegionId())
                .facilityTax(material.getFacilityTax())
                .buildingRig(material.getRigDiscount())
                .building(material.getBuildingDiscount())
                .tier(material.getTier())
                .build());
//        }
//        return material;
    }
}
