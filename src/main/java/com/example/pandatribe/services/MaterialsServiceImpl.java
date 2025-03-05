package com.example.pandatribe.services;

import com.example.pandatribe.models.industry.BuildingBonus;
import com.example.pandatribe.models.industry.RigBonus;
import com.example.pandatribe.models.industry.blueprints.BlueprintActivity;
import com.example.pandatribe.models.industry.blueprints.EveType;
import com.example.pandatribe.models.industry.blueprints.Material;
import com.example.pandatribe.models.market.ItemPrice;
import com.example.pandatribe.models.market.MarketPriceData;
import com.example.pandatribe.models.requests.MaterialInfo;
import com.example.pandatribe.repositories.interfaces.EveCustomRepository;
import com.example.pandatribe.repositories.interfaces.EveMaterialsRepository;
import com.example.pandatribe.repositories.interfaces.EveTypesRepository;
import com.example.pandatribe.services.contracts.MarketService;
import com.example.pandatribe.services.contracts.MaterialService;
import com.example.pandatribe.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MaterialsServiceImpl implements MaterialService {
    public static final Integer LOCATION_ID = 60003760;
    public static final String ORDER_TYPE = "all";
    private final EveTypesRepository eveTypesRepository;
    private final EveMaterialsRepository materialBlueprintRepository;
    private final EveCustomRepository eveCustomRepository;
    private final MarketService marketService;
    private final Helper helper;
    @Value("${REACTIONS}")
    private Boolean reactions;


    @Override
    @Transactional
    public List<MaterialInfo> getMaterialsByActivity(Integer blueprintId, Integer quantity, Integer discountBR, Integer materialEfficiency, Integer discountB, Double security, Integer blueprintCount, Integer regionId, Integer initialTier) {
        List<Material> materials = materialBlueprintRepository.findMaterialsByActivity(blueprintId);
        return getSimpleMaterials(materials, quantity, discountBR, materialEfficiency, discountB, security, blueprintCount, regionId, initialTier);
    }

    @Override
    public Integer getBlueprintComplexity(Integer blueprintId) {
        List<Material> materials = materialBlueprintRepository.findMaterialsByActivity(blueprintId);

        return materials.stream()
                .map(mat-> eveCustomRepository.getBluePrintInfoByProduct(mat.getBlueprintTypeId().getMaterialTypeId()))
                .filter(Objects::nonNull)
                .map(BlueprintActivity::getActivityId)
                .filter(activity -> activity == 1 || activity == 11)
                .findFirst()
                .map(activity -> activity == 1 ? 3 : 2)
                .orElse(1);
    }

    private List<MaterialInfo> getSimpleMaterials(List<Material> materials, Integer quantity, Integer discountBR, Integer materialEfficiency, Integer discountB, Double security, Integer blueprintCount, Integer regionId, Integer initialTier) {
        List<MaterialInfo> materialList = new ArrayList<>();
        BuildingBonus buildingBonus = helper.getBuildingBonus(discountB);
        RigBonus rigBonus = helper.getRigBonus(discountBR, discountB);
        List<MarketPriceData> marketPriceData = marketService.getMarketPriceData();
        Double rigMultiplier = getRigMultiplier(rigBonus, security);
        for (Material material : materials) {
            List<ItemPrice> marketItemPriceData = marketService.getItemMarketPrice(material.getBlueprintTypeId().getMaterialTypeId(),regionId, ORDER_TYPE);
            Optional<EveType> eveType = eveTypesRepository.findEveTypeByTypeId(material.getBlueprintTypeId().getMaterialTypeId());
            if (eveType.isEmpty()) {
                continue;
            }

            BlueprintActivity blueprintActivity = eveCustomRepository.getBluePrintInfoByProduct(eveType.get().getTypeId());
            Integer volume = eveCustomRepository.getVolume(eveType.get().getTypeId());
         //   Double craftQuantity = Optional.ofNullable(blueprintActivity).map(b -> Double.parseDouble(b.getCraftQuantity().toString())).orElse(1.0);
            Integer matQuantity = material.getQuantity() == 1 ? material.getQuantity() *
                    quantity : this.getQuantityDiscount(BigDecimal.valueOf((long) material.getQuantity() * quantity), rigBonus.getMaterialReduction() * rigMultiplier, materialEfficiency, buildingBonus.getMaterialReduction());
   //         Integer jobsCount = Objects.nonNull(blueprintActivity) ? (int) Math.ceil(matQuantity / craftQuantity) : matQuantity;
//            BlueprintResult.BlueprintResultBuilder materialDto = BlueprintResult.builder()
//                    .id(eveType.get().getTypeId())
//                    .name(eveType.get().getTypeName())
//                    .quantity(matQuantity)
//                    .excessMaterials(Objects.nonNull(blueprintActivity) ? Math.abs(craftQuantity*jobsCount-matQuantity) : 0)
//                    .craftQuantity(craftQuantity)
//                    .icon(helper.generateIconLink(eveType.get().getTypeId(),32))
//                    .sellPrice(marketService.getItemSellOrderPrice(LOCATION_ID, marketItemPriceData))
//                    .totalSellPrice(marketService.getItemSellOrderPrice(LOCATION_ID, marketItemPriceData).multiply(BigDecimal.valueOf(matQuantity)))
//                    .totalVolume((Objects.nonNull(volume) ? volume : eveType.get().getVolume()) * matQuantity)
//                    .volume(Objects.nonNull(volume) ? volume : eveType.get().getVolume())
//                    .activityId(Optional.ofNullable(blueprintActivity).map(BlueprintActivity::getActivityId).orElse(0))
//                    .adjustedPrice(marketPriceData.stream()
//                            .filter(m-> m.getTypeId().equals(eveType.get().getTypeId()))
//                            .findFirst()
//                            .map(MarketPriceData::getAdjustedPrice)
//                            .orElse(BigDecimal.ZERO).multiply(BigDecimal.valueOf(material.getQuantity())))
//                    .isFuel(eveType.get().getTypeName().contains("Fuel Block"))
//                    .tier(initialTier+1)
//                    .jobsCount(jobsCount);


//            if (Objects.isNull(blueprintActivity)) {
//                materialDto.isCreatable(Boolean.FALSE);
//                materialList.add(materialDto.build());
//                continue;
//            }
//            Boolean skip = reactions && blueprintActivity.getActivityId() == 11;
//            materialDto.isCreatable(skip ? Boolean.FALSE : Boolean.TRUE);
            materialList.add(MaterialInfo.builder().id(eveType.get().getTypeId())
                            .name(eveType.get().getTypeName())
                            .quantity(matQuantity*blueprintCount)
                            .volume(Objects.nonNull(volume) ? volume : eveType.get().getVolume())
                            .buyPrice(marketService.getItemPriceByOrderType("buy", marketItemPriceData))
                            .sellPrice(marketService.getItemPriceByOrderType("sell", marketItemPriceData))
                            .adjustedPrice(marketPriceData.stream()
                                .filter(m-> m.getTypeId().equals(eveType.get().getTypeId()))
                                    .findFirst()
                                    .map(MarketPriceData::getAdjustedPrice)
                                    .orElse(BigDecimal.ZERO).multiply(BigDecimal.valueOf((long)material.getQuantity()*quantity)))
                            .isCreatable(isCreatable(blueprintActivity))
                            .activityId(eveType.get().getTypeName().contains("Fuel Block") ? 105 : Optional.ofNullable(blueprintActivity).map(BlueprintActivity::getActivityId).orElse(0))
                            .tier(eveType.get().getTypeName().contains("Fuel Block")? 105 : initialTier + 1)
                            .icon(helper.generateIconLink(eveType.get().getTypeId(),32))
                            .build());
        }
        return materialList;
    }

    private Integer getQuantityDiscount(BigDecimal initialQuantity, Double discountBR, Integer discountBP, Integer discountB) {
        initialQuantity = initialQuantity.subtract(initialQuantity.multiply(BigDecimal.valueOf(discountBP / 100.0)));
        initialQuantity = initialQuantity.subtract(initialQuantity.multiply(BigDecimal.valueOf(discountB / 100.0)));
        initialQuantity = initialQuantity.subtract(initialQuantity.multiply(BigDecimal.valueOf(discountBR / 100.0)));
        return initialQuantity.setScale(0, RoundingMode.CEILING).intValue();
    }

    private Double getRigMultiplier(RigBonus rigBonus, Double security) {
        if (security >= 0.5) {
            return rigBonus.getHighSecMultiplier();
        }
        if (security < 0.5 && security > 0) {
            return rigBonus.getLowSecMultiplier();
        }
        return rigBonus.getNullSecMultiplier();
    }
    private Boolean isCreatable(BlueprintActivity blueprintActivity) {
        if(Objects.isNull(blueprintActivity)) {
            return Boolean.FALSE;
        }
        Boolean skip = reactions && blueprintActivity.getActivityId() == 11;
        return Boolean.TRUE.equals(skip) ? Boolean.FALSE : Boolean.TRUE;
    }
}
