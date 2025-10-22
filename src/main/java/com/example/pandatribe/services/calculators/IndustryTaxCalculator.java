package com.example.pandatribe.services.calculators;

import com.example.pandatribe.models.industry.CostIndex;
import com.example.pandatribe.models.requests.MaterialInfo;
import com.example.pandatribe.utils.IndustryBonusCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calculator for EVE Online industry taxes and costs.
 * Handles the complex calculations for manufacturing and reaction costs.
 */
@Component
@RequiredArgsConstructor
public class IndustryTaxCalculator {

    private final IndustryBonusCalculator industryBonusCalculator;

    private static final Double SURCHARGE_PERCENTAGE = 4.0;

    /**
     * Calculates total industry taxes for a manufacturing or reaction job.
     *
     * @param facilityPercent Facility tax percentage (set by structure owner)
     * @param costIndexes List of system cost indexes
     * @param materials List of materials with adjusted prices
     * @param activity Activity type ("manufacturing" or "reaction")
     * @param buildingIndex Building type index
     * @param count Number of runs
     * @return Total industry tax cost
     */
    public BigDecimal calculateIndustryTaxes(
            Double facilityPercent,
            List<CostIndex> costIndexes,
            List<MaterialInfo> materials,
            String activity,
            Integer buildingIndex,
            Integer count) {

        // Calculate Estimated Item Value (EIV)
        BigDecimal eiv = materials.stream()
                .map(MaterialInfo::getAdjustedPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get building bonus for cost reduction
        Integer buildingBonus = industryBonusCalculator.getBuildingBonus(buildingIndex)
                .getCostReduction();

        // Find system cost index for the specified activity
        Double costIndex = costIndexes.stream()
                .filter(c -> c.getActivity().equals(activity))
                .findFirst()
                .map(CostIndex::getCostIndex)
                .orElse(0.0);

        // Calculate base system cost
        BigDecimal systemCost = eiv.multiply(BigDecimal.valueOf(costIndex));

        // Apply building cost reduction
        BigDecimal buildingCostReduction = BigDecimal.valueOf(buildingBonus)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .multiply(systemCost);

        // Calculate facility tax
        BigDecimal facilityTax = BigDecimal.valueOf(facilityPercent / 100)
                .multiply(eiv)
                .setScale(0, RoundingMode.CEILING);

        // Calculate surcharge tax (4% of EIV)
        BigDecimal surChargeTax = BigDecimal.valueOf(SURCHARGE_PERCENTAGE / 100)
                .multiply(eiv)
                .setScale(0, RoundingMode.CEILING);

        // Calculate final cost
        BigDecimal finalCost = systemCost
                .subtract(buildingCostReduction)
                .add(facilityTax)
                .add(surChargeTax);

        return finalCost
                .setScale(0, RoundingMode.CEILING)
                .multiply(BigDecimal.valueOf(count));
    }

    /**
     * Calculates the base system cost component.
     *
     * @param estimatedItemValue The EIV of all materials
     * @param costIndex The system's cost index for the activity
     * @return System cost before bonuses
     */
    public BigDecimal calculateSystemCost(BigDecimal estimatedItemValue, Double costIndex) {
        return estimatedItemValue.multiply(BigDecimal.valueOf(costIndex));
    }

    /**
     * Calculates the building cost reduction.
     *
     * @param systemCost Base system cost
     * @param buildingBonusPercent Building bonus percentage
     * @return Amount of cost reduction from building bonus
     */
    public BigDecimal calculateBuildingCostReduction(BigDecimal systemCost, Integer buildingBonusPercent) {
        return BigDecimal.valueOf(buildingBonusPercent)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .multiply(systemCost);
    }
}
