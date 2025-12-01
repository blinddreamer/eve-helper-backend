package com.example.pandatribe.services;

import com.example.pandatribe.constants.EveConstants;
import com.example.pandatribe.exceptions.MaterialNotFoundException;
import com.example.pandatribe.models.industry.blueprints.BlueprintActivity;
import com.example.pandatribe.models.industry.blueprints.EveType;
import com.example.pandatribe.models.industry.blueprints.Material;
import com.example.pandatribe.models.market.ItemPrice;
import com.example.pandatribe.models.market.MarketPriceData;
import com.example.pandatribe.models.requests.MaterialInfo;
import com.example.pandatribe.repositories.BlueprintQueryRepository;
import com.example.pandatribe.repositories.interfaces.EveMaterialsRepository;
import com.example.pandatribe.repositories.interfaces.EveTypesRepository;
import com.example.pandatribe.services.builders.MaterialInfoBuilder;
import com.example.pandatribe.services.calculators.MaterialQuantityCalculator;
import com.example.pandatribe.services.contracts.MarketService;
import com.example.pandatribe.services.contracts.MaterialService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MaterialsServiceImpl implements MaterialService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialsServiceImpl.class);

    private final EveTypesRepository eveTypesRepository;
    private final EveMaterialsRepository materialBlueprintRepository;
    private final BlueprintQueryRepository blueprintQueryRepository;
    private final MarketService marketService;
    private final MaterialQuantityCalculator materialQuantityCalculator;
    private final MaterialInfoBuilder materialInfoBuilder;


    @Override
    @Transactional
    public List<MaterialInfo> getMaterialsByActivity(Integer blueprintId, Integer quantity, Integer discountBR, Integer materialEfficiency, Integer discountB, Double security, Integer blueprintCount, Integer regionId, Integer initialTier,
                                                     Long locationId) {
        List<Material> materials = materialBlueprintRepository.findMaterialsByActivity(blueprintId);
        return getSimpleMaterials(materials, quantity, discountBR, materialEfficiency, discountB, security, blueprintCount, regionId, initialTier, locationId);
    }

    @Override
    public Integer getBlueprintComplexity(Integer blueprintId) {
        List<Material> materials = materialBlueprintRepository.findMaterialsByActivity(blueprintId);

        return materials.stream()
                .map(mat-> blueprintQueryRepository.getBlueprintInfoByProduct(mat.getBlueprintTypeId().getMaterialTypeId()))
                .filter(Objects::nonNull)
                .map(BlueprintActivity::getActivityId)
                .filter(activity -> activity == 1 || activity == 11)
                .findFirst()
                .map(activity -> activity == 1 ? 3 : 2)
                .orElse(1);
    }

    /**
     * Processes a list of materials and enriches them with market data and calculated quantities.
     * Now uses dedicated calculator and builder for cleaner code.
     */
    private List<MaterialInfo> getSimpleMaterials(
            List<Material> materials,
            Integer quantity,
            Integer discountBR,
            Integer materialEfficiency,
            Integer discountB,
            Double security,
            Integer blueprintCount,
            Integer regionId,
            Integer initialTier,
            Long locationId) {

        List<MaterialInfo> materialList = new ArrayList<>();
        List<MarketPriceData> marketPriceData = marketService.getMarketPriceData();

        for (Material material : materials) {
            Integer materialTypeId = material.getBlueprintTypeId().getMaterialTypeId();

            // Fetch EVE type data
            EveType eveType = eveTypesRepository.findEveTypeByTypeId(materialTypeId)
                    .orElseThrow(() -> {
                        LOGGER.warn("Material type {} not found in EVE database", materialTypeId);
                        return new MaterialNotFoundException(materialTypeId);
                    });

            // Fetch market data
            List<ItemPrice> marketItemPriceData = marketService.getItemMarketPrice(
                materialTypeId, regionId, EveConstants.ORDER_TYPE_ALL
            );

            // Fetch blueprint and volume info
            BlueprintActivity blueprintActivity = blueprintQueryRepository.getBlueprintInfoByProduct(eveType.getTypeId());
            Double volume = blueprintQueryRepository.getVolume(eveType.getTypeId());

            // Calculate material quantity with all bonuses
            Integer matQuantity = materialQuantityCalculator.calculateMaterialQuantity(
                material.getQuantity(),
                material.getQuantity(),
                quantity,
                discountBR,
                materialEfficiency,
                discountB,
                security,
                blueprintCount
            );

            // Build complete MaterialInfo object
            MaterialInfo materialInfo = materialInfoBuilder.buildMaterialInfo(
                eveType,
                matQuantity,
                material.getQuantity() * quantity,
                volume,
                blueprintActivity,
                marketItemPriceData,
                marketPriceData,
                locationId,
                initialTier
            );

            materialList.add(materialInfo);
            LOGGER.debug("Processed material: {} (type ID: {}, quantity: {})",
                eveType.getTypeName(), materialTypeId, matQuantity);
        }

        return materialList;
    }
}
