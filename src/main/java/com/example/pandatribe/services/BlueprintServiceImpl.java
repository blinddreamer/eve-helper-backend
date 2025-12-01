package com.example.pandatribe.services;

import com.example.pandatribe.configs.EveDataConfig;
import com.example.pandatribe.constants.EveConstants;
import com.example.pandatribe.exceptions.BlueprintNotFoundException;
import com.example.pandatribe.exceptions.RequestNotFoundException;
import com.example.pandatribe.models.dbmodels.industry.BlueprintData;
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
import com.example.pandatribe.repositories.BlueprintQueryRepository;
import com.example.pandatribe.repositories.UniverseQueryRepository;
import com.example.pandatribe.repositories.interfaces.BlueprintDataRepository;
import com.example.pandatribe.repositories.interfaces.EveTypesRepository;
import com.example.pandatribe.services.calculators.BlueprintPriceCalculator;
import com.example.pandatribe.services.calculators.IndustryTaxCalculator;
import com.example.pandatribe.services.contracts.BlueprintService;
import com.example.pandatribe.services.contracts.IndustryService;
import com.example.pandatribe.services.contracts.MarketService;
import com.example.pandatribe.services.contracts.MaterialService;
import com.example.pandatribe.utils.EveImageService;
import com.example.pandatribe.utils.IndustryBonusCalculator;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@AllArgsConstructor
public class BlueprintServiceImpl implements BlueprintService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintServiceImpl.class);

    private final MaterialService materialsService;
    private final MarketService marketService;
    private final EveTypesRepository repository;
    private final BlueprintQueryRepository blueprintQueryRepository;
    private final UniverseQueryRepository universeQueryRepository;
    private final IndustryService industryService;
    private final EveDataConfig eveDataConfig;
    private final IndustryBonusCalculator industryBonusCalculator;
    private final EveImageService eveImageService;
    private final IndustryTaxCalculator industryTaxCalculator;
    private final BlueprintPriceCalculator blueprintPriceCalculator;
    private final BlueprintDataRepository blueprintDataRepository;
    private final ApplicationContext applicationContext;

    @Override
    public BlueprintData getInitialBlueprintData(BlueprintRequest searchDto) {
        BlueprintServiceImpl self = applicationContext.getBean(BlueprintServiceImpl.class);
        BlueprintResult initialBlueprint = self.getBlueprintData(searchDto);
        if (Objects.isNull(initialBlueprint)) {
            throw new BlueprintNotFoundException(searchDto.getBlueprintName());
        }
        return blueprintDataRepository.saveAndFlush(
                BlueprintData.builder().id(UUID.randomUUID().toString())
                        .blueprintResult(Collections.singletonList(initialBlueprint))
                        .creationDate(LocalDate.now()).build());
    }

    public BlueprintData massUpdateMaterials(List<BlueprintRequest> requests) {
        String requestId = requests.get(0).getRequestId();
        BlueprintData blueprintData = blueprintDataRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
        requests.forEach(request -> updateBlueprintData(blueprintData, request));
        return blueprintDataRepository.saveAndFlush(blueprintData);
    }

    @Override
    public BlueprintData updateSubMaterials(BlueprintRequest subMaterialsRequest) {
        String requestId = subMaterialsRequest.getRequestId();
        BlueprintData blueprintData = blueprintDataRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
        return blueprintDataRepository.saveAndFlush(updateBlueprintData(blueprintData, subMaterialsRequest));
    }

    @Override
    @Cacheable(value = "blueprints", cacheManager = "staticDataCacheManager")
    public GetBlueprintsResult getEveBlueprints() {
        List<Blueprint> blueprints = blueprintQueryRepository.getAllBlueprints();
        LOGGER.info("Blueprints loaded - {}", !blueprints.isEmpty());

        return GetBlueprintsResult.builder()
                .blueprints(blueprints.stream().map(bp ->
                        bp.withComplexity(materialsService.getBlueprintComplexity(bp.getBpId()))).toList())
                .build();
    }

    @Override
    @Cacheable(value = "systemNames", cacheManager = "staticDataCacheManager")
    public List<SystemName> getEveSystems() {
        List<SystemName> systems = universeQueryRepository.getAllSystems();
        LOGGER.info("Systems loaded - {}", !systems.isEmpty());
        return systems;
    }

    @Override
    @Cacheable(value = "regions", cacheManager = "staticDataCacheManager")
    public List<Region> getEveRegions() {
        List<Region> regions = universeQueryRepository.getAllRegions();
        LOGGER.info("Regions loaded - {}", !regions.isEmpty());
        return regions;
    }

    @Override
    @Cacheable(value = "stations", cacheManager = "staticDataCacheManager")
    public List<Station> getEveStations() {
        List<Station> stations = universeQueryRepository.getFeaturedStations().stream()
                .sorted(Comparator.comparing(Station::getRegionName, Comparator.nullsLast(String::compareTo))).toList();
        LOGGER.info("Stations loaded - {}", !stations.isEmpty());
        return stations;
    }

    /**
     * Calculates industry taxes for a manufacturing or reaction job.
     * Delegates to IndustryTaxCalculator for the actual calculation.
     *
     * @param facilityPercent Facility tax percentage
     * @param systemId Solar system ID
     * @param materials List of materials with prices
     * @param activity Activity type ("manufacturing" or "reaction")
     * @param buildingIndex Building type index
     * @param count Number of runs
     * @return Total industry tax cost
     */
    private BigDecimal calculateIndustryTaxes(
            Double facilityPercent,
            Integer systemId,
            List<MaterialInfo> materials,
            String activity,
            Integer buildingIndex,
            Integer count) {

        // Get system cost indexes
        List<CostIndex> costIndexes = industryService.getSystemCostIndexes().stream()
                .filter(c -> c.getSystemId().equals(systemId))
                .flatMap(systemIndex -> systemIndex.getCostIndexes().stream())
                .toList();

        return industryTaxCalculator.calculateIndustryTaxes(
                facilityPercent,
                costIndexes,
                materials,
                activity,
                buildingIndex,
                count
        );
    }

    @Cacheable(
        value = "cacheCalculator",
        key = "#blueprintRequest.blueprintName + '_' + " +
              "#blueprintRequest.runs + '_' + " +
              "#blueprintRequest.blueprintMe + '_' + " +
              "#blueprintRequest.regionId",
        unless = "#result == null"
    )
    public BlueprintResult getBlueprintData(BlueprintRequest blueprintRequest) {
        Boolean init = Optional.ofNullable(blueprintRequest.getInit()).orElse(false);
        Integer runs = Optional.ofNullable(blueprintRequest.getRuns()).orElse(1);
        Integer blueprintMaterialEfficiency = Optional.ofNullable(blueprintRequest.getBlueprintMe()).orElse(0);
        Integer rigDiscount = Optional.ofNullable(blueprintRequest.getBuildingRig()).orElse(0);
        Integer buildingDiscount = Optional.ofNullable(blueprintRequest.getBuilding()).orElse(0);
        String system = Optional.ofNullable(blueprintRequest.getSystem())
                .filter(s -> !s.isEmpty())
                .orElse(eveDataConfig.getDefaultSystem());
        Double facilityTax = Optional.ofNullable(blueprintRequest.getFacilityTax()).orElse(0.0);
        String blueprintName = blueprintRequest.getBlueprintName();
        Integer count = Optional.ofNullable(blueprintRequest.getCount()).orElse(1);
        Long locationId = Optional.ofNullable(blueprintRequest.getRegionId())
                .map(s-> s.split("_")[1])
                .map(Long::parseLong)
                .orElse(eveDataConfig.getDefaultLocationId());
        Integer regionId = Optional.ofNullable(blueprintRequest.getRegionId())
                .map(s-> s.split("_")[0])
                .map(Integer::parseInt)
                .orElse(eveDataConfig.getDefaultRegionId());
        Integer tier = Optional.ofNullable(blueprintRequest.getTier()).orElse(0);
        EveType eveType = repository.findEveTypeByTypeName(blueprintName).stream().findFirst().orElse(null);
        if (Objects.isNull(eveType)) {
            return null;
        }
        Integer size = Boolean.TRUE.equals(init) ? EveConstants.ICON_SIZE_HUGE : EveConstants.ICON_SIZE_SMALL;
        BlueprintActivity blueprintActivity = blueprintQueryRepository.getBlueprintInfoByProduct(eveType.getTypeId());
        if (Objects.nonNull(blueprintActivity)) {
            SystemInfo systemInfo = universeQueryRepository.getSystemInfo(system);
            if (Objects.isNull(systemInfo)) {
                systemInfo = universeQueryRepository.getSystemInfo(eveDataConfig.getDefaultSystem());
            }
            Double volume = blueprintQueryRepository.getVolume(eveType.getTypeId());
            Integer matBlueprintId = blueprintActivity.getBlueprintId();
            Integer craftCount = (int) Math.ceil((double) runs / blueprintActivity.getCraftQuantity());
            Double craftQuantity = Optional.of(blueprintActivity).map(b -> Double.parseDouble(b.getCraftQuantity().toString())).orElse(1.0);
            List<MaterialInfo> materialsList = materialsService.getMaterialsByActivity(matBlueprintId, craftCount, rigDiscount, blueprintMaterialEfficiency, buildingDiscount, systemInfo.getSecurity(), count, regionId, tier, locationId);
            String activity = blueprintActivity.getActivityId().equals(EveConstants.REACTION_ACTIVITY_ID)
                    ? EveConstants.REACTION : EveConstants.MANUFACTURING;
            BigDecimal industryCosts = calculateIndustryTaxes(facilityTax, systemInfo.getSystemId(),
                    materialsList, activity, buildingDiscount, count);
            List<ItemPrice> itemPriceList = marketService.getItemMarketPrice(eveType.getTypeId(), regionId,
                    EveConstants.ORDER_TYPE_ALL);
            BigDecimal buyPrice = marketService
                    .getItemPriceByOrderType(EveConstants.ORDER_TYPE_BUY, itemPriceList, locationId);
            BigDecimal sellPrice = marketService
                    .getItemPriceByOrderType(EveConstants.ORDER_TYPE_SELL, itemPriceList, locationId);
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
                    .craftQuantity(craftQuantity)
                    .tier(tier)
                    .isFuel(blueprintName.contains("Fuel Block"))
                    .blueprintMaterialEfficiency(blueprintMaterialEfficiency)
                    .facilityTax(facilityTax)
                    .regionStation(blueprintRequest.getRegionId())
                    .system(system)
                    .buildingDiscount(buildingDiscount)
                    .selectedForCraft(Boolean.TRUE)
                    .rigDiscount(rigDiscount)
                    .icon(eveType.getGroupId().equals(EveConstants.SHIP_GROUP_ID)
                            ? eveImageService.generateRenderLink(eveType.getTypeId(), size)
                            : eveImageService.generateIconLink(eveType.getTypeId(), size))
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
        blueprintData.getBlueprintResult().forEach(result ->
            initialQuantities.put(result.getName(), result.getQuantity())
        );
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
            BlueprintResult initialBlueprint = newData.get(0);
            BigDecimal buyCraftPrice = recalculateMasterCraftingPrice(blueprintData, true).add(initialBlueprint.getIndustryCosts());
            BigDecimal sellCraftPrice = recalculateMasterCraftingPrice(blueprintData, false).add(initialBlueprint.getIndustryCosts());
            initialBlueprint.setBuyCraftPrice(buyCraftPrice);
            initialBlueprint.setSellCraftPrice(sellCraftPrice);
            return blueprintData.withBlueprintResult(newData);
        }
    }

    /**
     * Recalculates master blueprint crafting price.
     * Delegates to BlueprintPriceCalculator.
     */
    private BigDecimal recalculateMasterCraftingPrice(BlueprintData blueprintData, Boolean isBuyPrice) {
        return blueprintPriceCalculator.recalculateMasterCraftingPrice(blueprintData, isBuyPrice);
    }

    /**
     * Recalculates sub-materials crafting prices recursively.
     * Delegates to BlueprintPriceCalculator.
     */
    private BigDecimal recalculateSubMaterialsCraftingPrices(
            List<MaterialInfo> materialsList,
            List<BlueprintResult> selectedForCraftList,
            Boolean isBuyPrice) {
        return blueprintPriceCalculator.recalculateSubMaterialsCraftingPrices(
                materialsList,
                selectedForCraftList,
                isBuyPrice
        );
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

    /**
     * Calculates total quantity needed for a material.
     * Delegates to BlueprintPriceCalculator.
     */
    private Integer calculateQuantity(List<BlueprintResult> originalData, String blueprintName) {
        return blueprintPriceCalculator.calculateQuantity(originalData, blueprintName);
    }

    private BlueprintResult updateNeededMaterials(List<BlueprintResult> originalData, BlueprintResult material,
                                                  Map<String, Integer> initialQuantities) {
        BlueprintServiceImpl self = applicationContext.getBean(BlueprintServiceImpl.class);
        Integer quant = calculateQuantity(originalData, material.getName());

        if (quant == 0 || Boolean.FALSE.equals(material.getSelectedForCraft())) {
            return material.withSelectedForCraft(Boolean.FALSE);
        }
        return self.getBlueprintData(BlueprintRequest.builder()
                .blueprintName(material.getName())
                .runs(quant)
                .blueprintMe(material.getBlueprintMaterialEfficiency())
                .system(material.getSystem())
                .regionId(material.getRegionStation())
                .facilityTax(material.getFacilityTax())
                .buildingRig(material.getRigDiscount())
                .building(material.getBuildingDiscount())
                .tier(material.getTier())
                .build());
    }
}
