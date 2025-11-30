package com.example.pandatribe.services.builders;

import com.example.pandatribe.constants.EveConstants;
import com.example.pandatribe.models.industry.blueprints.BlueprintActivity;
import com.example.pandatribe.models.industry.blueprints.EveType;
import com.example.pandatribe.models.market.ItemPrice;
import com.example.pandatribe.models.market.MarketPriceData;
import com.example.pandatribe.models.requests.MaterialInfo;
import com.example.pandatribe.services.contracts.MarketService;
import com.example.pandatribe.utils.EveImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Builder for MaterialInfo objects with complete market data, pricing, and metadata.
 * Encapsulates the complex logic of building material information from multiple data sources.
 */
@Component
@RequiredArgsConstructor
public class MaterialInfoBuilder {

    private final MarketService marketService;
    private final EveImageService eveImageService;

    @Value("${REACTIONS}")
    private Boolean reactions;

    /**
     * Builds a complete MaterialInfo object from EVE type data and market information.
     *
     * @param eveType The EVE type entity
     * @param quantity The calculated material quantity
     * @param baseQuantity The base quantity from blueprint
     * @param volume The item volume (m³)
     * @param blueprintActivity The blueprint activity (manufacturing/reaction)
     * @param marketItemPriceData Market prices for this item
     * @param marketPriceData Global adjusted prices
     * @param locationId The location ID for market orders
     * @param initialTier The current crafting tier
     * @return Complete MaterialInfo object
     */
    public MaterialInfo buildMaterialInfo(
            EveType eveType,
            Integer quantity,
            Integer baseQuantity,
            Integer volume,
            BlueprintActivity blueprintActivity,
            List<ItemPrice> marketItemPriceData,
            List<MarketPriceData> marketPriceData,
            Long locationId,
            Integer initialTier) {

        return MaterialInfo.builder()
                .id(eveType.getTypeId())
                .name(eveType.getTypeName())
                .quantity(quantity)
                .volume(Objects.nonNull(volume) ? volume : eveType.getVolume())
                .buyPrice(marketService.getItemPriceByOrderType("buy", marketItemPriceData, locationId))
                .sellPrice(marketService.getItemPriceByOrderType("sell", marketItemPriceData, locationId))
                .adjustedPrice(calculateAdjustedPrice(eveType.getTypeId(), baseQuantity, marketPriceData))
                .isCreatable(isCreatable(blueprintActivity))
                .activityId(determineActivityId(eveType.getTypeName(), blueprintActivity))
                .tier(determineTier(eveType.getTypeName(), initialTier))
                .icon(eveImageService.generateIconLink(eveType.getTypeId(), 32))
                .build();
    }

    /**
     * Calculates the adjusted price for a material based on EVE's global pricing.
     *
     * @param typeId The type ID
     * @param baseQuantity The base quantity
     * @param marketPriceData The global market price data
     * @return The adjusted price multiplied by quantity
     */
    private BigDecimal calculateAdjustedPrice(
            Integer typeId,
            Integer baseQuantity,
            List<MarketPriceData> marketPriceData) {

        return marketPriceData.stream()
                .filter(m -> m.getTypeId().equals(typeId))
                .findFirst()
                .map(MarketPriceData::getAdjustedPrice)
                .orElse(BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(baseQuantity));
    }

    /**
     * Determines if a material can be crafted.
     * Materials with blueprints are creatable, unless they're reactions and reactions are disabled.
     *
     * @param blueprintActivity The blueprint activity
     * @return true if the material can be crafted
     */
    private Boolean isCreatable(BlueprintActivity blueprintActivity) {
        if (Objects.isNull(blueprintActivity)) {
            return Boolean.FALSE;
        }

        // Check if this is a reaction
        boolean isReaction = blueprintActivity.getActivityId().equals(EveConstants.REACTION_ACTIVITY_ID);

        // If it's a reaction and reactions are disabled, it's not creatable
        if (isReaction && Boolean.FALSE.equals(reactions)) {
            return Boolean.FALSE;
        }

        // Everything else is creatable
        return Boolean.TRUE;
    }

    /**
     * Determines the activity ID for a material.
     * Special handling for Fuel Blocks which use activity ID 105.
     *
     * @param typeName The name of the material
     * @param blueprintActivity The blueprint activity
     * @return The activity ID
     */
    private Integer determineActivityId(String typeName, BlueprintActivity blueprintActivity) {
        // Fuel Blocks have special activity ID
        if (typeName.contains("Fuel Block")) {
            return EveConstants.FUEL_BLOCK_ACTIVITY_ID;
        }

        return Optional.ofNullable(blueprintActivity)
                .map(BlueprintActivity::getActivityId)
                .orElse(0);
    }

    /**
     * Determines the crafting tier for a material.
     * Fuel Blocks are special and get tier 105, others increment from initial tier.
     *
     * @param typeName The name of the material
     * @param initialTier The current tier
     * @return The tier for this material
     */
    private Integer determineTier(String typeName, Integer initialTier) {
        if (typeName.contains("Fuel Block")) {
            return EveConstants.FUEL_BLOCK_TIER;
        }

        return initialTier + 1;
    }
}
