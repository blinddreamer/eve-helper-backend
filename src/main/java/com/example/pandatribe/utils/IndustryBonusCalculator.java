package com.example.pandatribe.utils;

import com.example.pandatribe.models.industry.BuildingBonus;
import com.example.pandatribe.models.industry.RigBonus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Calculator for EVE Online industry bonuses.
 * Provides building and rig bonus calculations for manufacturing and reactions.
 */
@Component
public class IndustryBonusCalculator {

    private final Map<Integer, BuildingBonus> buildingBonuses;
    private final Map<Integer, RigBonus> rigBonuses;

    public IndustryBonusCalculator() {
        this.buildingBonuses = initializeBuildingBonuses();
        this.rigBonuses = initializeRigBonuses();
    }

    /**
     * Retrieves building bonus by index.
     *
     * @param index Building type index (0-5)
     * @return BuildingBonus containing cost and material reduction percentages
     */
    public BuildingBonus getBuildingBonus(Integer index) {
        return buildingBonuses.getOrDefault(index, buildingBonuses.get(0));
    }

    /**
     * Retrieves rig bonus by index and building type.
     * The rig index is adjusted based on the building type.
     *
     * @param rigIndex Rig type index (0-4)
     * @param buildingIndex Building type index (used to adjust rig index)
     * @return RigBonus containing material reduction and security multipliers
     */
    public RigBonus getRigBonus(Integer rigIndex, Integer buildingIndex) {
        // Adjust rig index based on building type
        Integer adjustedIndex = buildingIndex > 3 ? rigIndex + 2 : rigIndex;
        return rigBonuses.getOrDefault(adjustedIndex, rigBonuses.get(0));
    }

    /**
     * Calculates the rig multiplier based on system security status.
     *
     * @param rigBonus The rig bonus object
     * @param security System security status (-1.0 to 1.0), null defaults to high-sec (1.0)
     * @return Multiplier for the given security level
     */
    public Double calculateRigMultiplier(RigBonus rigBonus, Double security) {
        // Default to high-sec if security is null
        if (security == null) {
            security = 1.0;
        }

        if (security >= 0.5) {
            return rigBonus.getHighSecMultiplier();
        } else if (security > 0) {
            return rigBonus.getLowSecMultiplier();
        } else {
            return rigBonus.getNullSecMultiplier();
        }
    }

    /**
     * Initializes building bonuses map.
     * Each building type has different cost and material reduction bonuses.
     */
    private Map<Integer, BuildingBonus> initializeBuildingBonuses() {
        Map<Integer, BuildingBonus> bonuses = new HashMap<>();

        // Index 0: No building bonus
        bonuses.put(0, BuildingBonus.builder()
                .costReduction(0)
                .materialReduction(0)
                .build());

        // Index 1: Raitaru (Medium Engineering Complex)
        bonuses.put(1, BuildingBonus.builder()
                .costReduction(4)
                .materialReduction(1)
                .build());

        // Index 2: Azbel (Large Engineering Complex)
        bonuses.put(2, BuildingBonus.builder()
                .costReduction(3)
                .materialReduction(1)
                .build());

        // Index 3: Sotiyo (XL Engineering Complex)
        bonuses.put(3, BuildingBonus.builder()
                .costReduction(5)
                .materialReduction(1)
                .build());

        // Index 4: Athanor (Medium Refinery)
        bonuses.put(4, BuildingBonus.builder()
                .costReduction(0)
                .materialReduction(0)
                .build());

        // Index 5: Tatara (Large Refinery)
        bonuses.put(5, BuildingBonus.builder()
                .costReduction(0)
                .materialReduction(0)
                .build());

        return bonuses;
    }

    /**
     * Initializes rig bonuses map.
     * Each rig type has different material reduction and security multipliers.
     */
    private Map<Integer, RigBonus> initializeRigBonuses() {
        Map<Integer, RigBonus> bonuses = new HashMap<>();

        // Index 0: No rig
        bonuses.put(0, RigBonus.builder()
                .materialReduction(0.0)
                .highSecMultiplier(1.0)
                .lowSecMultiplier(1.9)
                .nullSecMultiplier(2.1)
                .build());

        // Index 1: T1 Manufacturing Rig
        bonuses.put(1, RigBonus.builder()
                .materialReduction(2.0)
                .highSecMultiplier(1.0)
                .lowSecMultiplier(1.9)
                .nullSecMultiplier(2.1)
                .build());

        // Index 2: T2 Manufacturing Rig
        bonuses.put(2, RigBonus.builder()
                .materialReduction(2.4)
                .highSecMultiplier(1.0)
                .lowSecMultiplier(1.9)
                .nullSecMultiplier(2.1)
                .build());

        // Index 3: T1 Reaction Rig
        bonuses.put(3, RigBonus.builder()
                .materialReduction(2.0)
                .highSecMultiplier(0.0)
                .lowSecMultiplier(1.0)
                .nullSecMultiplier(1.1)
                .build());

        // Index 4: T2 Reaction Rig
        bonuses.put(4, RigBonus.builder()
                .materialReduction(2.4)
                .highSecMultiplier(0.0)
                .lowSecMultiplier(1.0)
                .nullSecMultiplier(1.1)
                .build());

        return bonuses;
    }
}
