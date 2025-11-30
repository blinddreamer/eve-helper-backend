package com.example.pandatribe.services.calculators;

import com.example.pandatribe.models.industry.BuildingBonus;
import com.example.pandatribe.models.industry.RigBonus;
import com.example.pandatribe.utils.IndustryBonusCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculator for material quantities with EVE Online industry bonuses.
 * Handles complex material quantity calculations including:
 * - Material Efficiency (ME) from blueprint research
 * - Building bonuses from manufacturing structures
 * - Rig bonuses from structure rigs
 * - Security status multipliers
 */
@Component
@RequiredArgsConstructor
public class MaterialQuantityCalculator {

    private final IndustryBonusCalculator industryBonusCalculator;

    /**
     * Calculates the final material quantity after applying all bonuses and discounts.
     *
     * @param baseQuantity The base quantity from the blueprint
     * @param materialQuantity The material quantity per run
     * @param quantity The number of runs
     * @param discountBR The rig bonus index
     * @param materialEfficiency The blueprint ME level (0-10)
     * @param discountB The building bonus index
     * @param security The system security status (-1.0 to 1.0)
     * @param blueprintCount The number of blueprint runs
     * @return The final material quantity needed
     */
    public Integer calculateMaterialQuantity(
            Integer baseQuantity,
            Integer materialQuantity,
            Integer quantity,
            Integer discountBR,
            Integer materialEfficiency,
            Integer discountB,
            Double security,
            Integer blueprintCount) {

        // Special case: quantity 1 materials are not affected by ME or bonuses
        if (materialQuantity == 1) {
            return materialQuantity * quantity * blueprintCount;
        }

        BuildingBonus buildingBonus = industryBonusCalculator.getBuildingBonus(discountB);
        RigBonus rigBonus = industryBonusCalculator.getRigBonus(discountBR, discountB);
        Double rigMultiplier = industryBonusCalculator.calculateRigMultiplier(rigBonus, security);

        BigDecimal totalQuantity = BigDecimal.valueOf((long) materialQuantity * quantity);
        BigDecimal finalQuantity = applyDiscounts(
            totalQuantity,
            rigBonus.getMaterialReduction() * rigMultiplier,
            materialEfficiency,
            buildingBonus.getMaterialReduction()
        );

        return finalQuantity.intValue() * blueprintCount;
    }

    /**
     * Applies all discount percentages to the base quantity.
     * Order: Blueprint ME → Building bonus → Rig bonus
     *
     * @param initialQuantity The starting quantity
     * @param discountBR The rig bonus percentage (already multiplied by security)
     * @param discountBP The blueprint ME percentage (0-10%)
     * @param discountB The building bonus percentage
     * @return The final quantity after all discounts, rounded up
     */
    public BigDecimal applyDiscounts(
            BigDecimal initialQuantity,
            Double discountBR,
            Integer discountBP,
            Integer discountB) {

        // Apply Material Efficiency (ME) from blueprint research
        initialQuantity = initialQuantity.subtract(
            initialQuantity.multiply(BigDecimal.valueOf(discountBP / 100.0))
        );

        // Apply building bonus from manufacturing structure
        initialQuantity = initialQuantity.subtract(
            initialQuantity.multiply(BigDecimal.valueOf(discountB / 100.0))
        );

        // Apply rig bonus from structure rigs (already includes security multiplier)
        initialQuantity = initialQuantity.subtract(
            initialQuantity.multiply(BigDecimal.valueOf(discountBR / 100.0))
        );

        // Always round up - you can't manufacture partial items
        return initialQuantity.setScale(0, RoundingMode.CEILING);
    }

    /**
     * Calculates the total quantity needed across multiple blueprint copies.
     *
     * @param singleRunQuantity The quantity needed for a single run
     * @param runs The number of blueprint runs
     * @param copies The number of blueprint copies
     * @return The total quantity needed
     */
    public Integer calculateTotalQuantity(Integer singleRunQuantity, Integer runs, Integer copies) {
        return singleRunQuantity * runs * copies;
    }

    /**
     * Gets the rig multiplier based on security status.
     * This determines how effective structure rigs are in different security spaces.
     *
     * @param rigBonus The rig bonus configuration
     * @param security The system security status
     * @return The security-based multiplier
     */
    public Double getRigMultiplier(RigBonus rigBonus, Double security) {
        return industryBonusCalculator.calculateRigMultiplier(rigBonus, security);
    }
}
