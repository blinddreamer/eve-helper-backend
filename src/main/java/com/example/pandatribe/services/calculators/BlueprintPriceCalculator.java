package com.example.pandatribe.services.calculators;

import com.example.pandatribe.models.dbmodels.industry.BlueprintData;
import com.example.pandatribe.models.requests.MaterialInfo;
import com.example.pandatribe.models.results.BlueprintResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Calculator for blueprint crafting prices.
 * Handles recursive price calculations for nested blueprint materials.
 */
@Component
public class BlueprintPriceCalculator {

    /**
     * Recalculates the master blueprint crafting price.
     * Takes into account which sub-materials are selected for crafting.
     *
     * @param blueprintData The blueprint data containing all materials
     * @param useBuyPrice Whether to use buy prices (true) or sell prices (false)
     * @return Total crafting cost
     */
    public BigDecimal recalculateMasterCraftingPrice(BlueprintData blueprintData, Boolean useBuyPrice) {
        List<MaterialInfo> initialMatList = blueprintData.getBlueprintResult().get(0).getMaterialsList();
        List<BlueprintResult> selectedForCraftList = blueprintData.getBlueprintResult();

        return initialMatList.stream()
                .map(mat -> calculateMaterialCost(mat, selectedForCraftList, useBuyPrice))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the cost for a single material.
     * If the material is selected for crafting, recursively calculates its crafting cost.
     * Otherwise, uses the market buy price.
     *
     * @param material The material to calculate cost for
     * @param selectedForCraftList List of blueprints selected for crafting
     * @param useBuyPrice Whether to use buy prices
     * @return Cost for this material
     */
    private BigDecimal calculateMaterialCost(
            MaterialInfo material,
            List<BlueprintResult> selectedForCraftList,
            Boolean useBuyPrice) {

        BlueprintResult existingMat = selectedForCraftList.stream()
                .filter(bp -> bp.getName().equals(material.getName()))
                .findFirst()
                .orElse(null);

        if (Objects.nonNull(existingMat)) {
            if (Boolean.TRUE.equals(existingMat.getSelectedForCraft())) {
                // Recursively calculate crafting cost for this sub-material
                return recalculateSubMaterialsCraftingPrices(
                        existingMat.getMaterialsList(),
                        selectedForCraftList,
                        useBuyPrice
                ).add(existingMat.getIndustryCosts());
            } else {
                // Use market price
                return material.getBuyPrice().multiply(BigDecimal.valueOf(material.getQuantity()));
            }
        } else {
            // Material not in craft list, calculate quantity needed
            Integer quantity = calculateQuantity(selectedForCraftList, material.getName());
            return material.getBuyPrice().multiply(BigDecimal.valueOf(quantity));
        }
    }

    /**
     * Recursively calculates crafting prices for sub-materials.
     *
     * @param materialsList Materials needed for this blueprint
     * @param selectedForCraftList List of blueprints selected for crafting
     * @param useBuyPrice Whether to use buy prices
     * @return Total cost for all sub-materials
     */
    public BigDecimal recalculateSubMaterialsCraftingPrices(
            List<MaterialInfo> materialsList,
            List<BlueprintResult> selectedForCraftList,
            Boolean useBuyPrice) {

        return materialsList.stream()
                .map(mat -> {
                    BlueprintResult existingMat = selectedForCraftList.stream()
                            .filter(bp -> bp.getName().equals(mat.getName()))
                            .findFirst()
                            .orElse(null);

                    if (Objects.nonNull(existingMat)) {
                        if (Boolean.TRUE.equals(existingMat.getSelectedForCraft())) {
                            // Recursive call for nested materials
                            return recalculateSubMaterialsCraftingPrices(
                                    existingMat.getMaterialsList(),
                                    selectedForCraftList,
                                    useBuyPrice
                            ).add(existingMat.getIndustryCosts());
                        } else {
                            return mat.getBuyPrice().multiply(BigDecimal.valueOf(mat.getQuantity()));
                        }
                    } else {
                        return mat.getBuyPrice().multiply(BigDecimal.valueOf(mat.getQuantity()));
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the total quantity needed for a material across all selected blueprints.
     *
     * @param originalData List of all blueprint data
     * @param materialName Name of the material to calculate quantity for
     * @return Total quantity needed
     */
    public Integer calculateQuantity(List<BlueprintResult> originalData, String materialName) {
        return originalData.stream()
                .filter(mat -> mat.getSelectedForCraft() &&
                        mat.getMaterialsList().stream()
                                .anyMatch(m -> m.getName().equals(materialName)))
                .flatMap(mat -> mat.getMaterialsList().stream())
                .filter(m -> m.getName().equals(materialName))
                .map(MaterialInfo::getQuantity)
                .reduce(Integer::sum)
                .orElse(0);
    }
}
