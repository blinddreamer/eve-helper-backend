package com.example.pandatribe.repositories;

import com.example.pandatribe.models.industry.blueprints.PiDependency;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Planetary Interaction (PI) queries.
 * Handles PI schematics, materials, and production chains.
 */
@Repository
public class PlanetaryInteractionRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlanetaryInteractionRepository.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Retrieves all unique material type IDs used in planetary interaction schematics.
     *
     * @return List of type IDs for PI materials
     */
    @Transactional(readOnly = true)
    public List<Integer> getRawMaterials() {
        String nativeQuery = "SELECT DISTINCT typeID FROM planetSchematicsTypeMap";
        List<Tuple> result = entityManager.createNativeQuery(nativeQuery, Tuple.class)
                .getResultList();

        LOGGER.debug("Retrieved {} raw PI materials", result.size());

        return result.isEmpty() ? new ArrayList<>() :
                result.stream()
                        .map(row -> (Integer) row.get("typeID"))
                        .toList();
    }

    /**
     * Retrieves the input/output dependencies for a planetary interaction schematic.
     *
     * @param schematicID The schematic ID
     * @return List of materials with quantities and input/output flags
     */
    @Transactional(readOnly = true)
    public List<PiDependency> getPiDependencies(Integer schematicID) {
        String nativeQuery = "SELECT DISTINCT quantity, typeID, isInput " +
                "FROM planetSchematicsTypeMap " +
                "WHERE schematicID = :schematicID";

        List<Tuple> result = entityManager.createNativeQuery(nativeQuery, Tuple.class)
                .setParameter("schematicID", schematicID)
                .getResultList();

        return result.isEmpty() ? new ArrayList<>() :
                result.stream()
                        .map(row -> PiDependency.builder()
                                .typeID((Integer) row.get("typeID"))
                                .isInput(((Number) row.get("isInput")).intValue() == 1)
                                .quantity((Integer) row.get("quantity"))
                                .build())
                        .toList();
    }

    /**
     * Retrieves the schematic ID that produces a given type.
     *
     * @param typeId The type ID of the output product
     * @return Schematic ID, or null if not found
     */
    @Transactional(readOnly = true)
    public Integer getSchematicId(Integer typeId) {
        String nativeQuery = "SELECT schematicID " +
                "FROM planetSchematicsTypeMap " +
                "WHERE typeID = :typeId AND isInput = false";

        List<Tuple> result = entityManager.createNativeQuery(nativeQuery, Tuple.class)
                .setParameter("typeId", typeId)
                .getResultList();

        return result.isEmpty() ? null :
                result.stream()
                        .findFirst()
                        .map(row -> (Integer) row.get("schematicID"))
                        .orElse(null);
    }

    /**
     * Retrieves the cycle time for a planetary interaction schematic.
     *
     * @param schematicId The schematic ID
     * @return Cycle time in seconds, or null if not found
     */
    @Transactional(readOnly = true)
    public Integer getCycleTime(Integer schematicId) {
        String nativeQuery = "SELECT cycleTime " +
                "FROM planetSchematics " +
                "WHERE schematicID = :schematicId";

        List<Tuple> result = entityManager.createNativeQuery(nativeQuery, Tuple.class)
                .setParameter("schematicId", schematicId)
                .getResultList();

        return result.isEmpty() ? null :
                result.stream()
                        .findFirst()
                        .map(row -> (Integer) row.get("cycleTime"))
                        .orElse(null);
    }
}
