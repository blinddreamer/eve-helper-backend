package com.example.pandatribe.repositories;

import com.example.pandatribe.models.industry.blueprints.BlueprintActivity;
import com.example.pandatribe.models.results.Blueprint;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Repository for blueprint-related queries.
 * Handles blueprint information, products, and activities.
 */
@Repository
@RequiredArgsConstructor
public class BlueprintQueryRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintQueryRepository.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Retrieves blueprint information by product ID.
     * Finds the blueprint that produces the specified product.
     *
     * @param productId The type ID of the product
     * @return BlueprintActivity containing blueprint details, or null if not found
     */
    @Transactional(readOnly = true)
    public BlueprintActivity getBlueprintInfoByProduct(Integer productId) {
        String nativeQuery = "SELECT quantity, typeID, activityID " +
                "FROM industryActivityProducts " +
                "WHERE productTypeID = :productId";

        List<Object[]> result = entityManager.createNativeQuery(nativeQuery)
                .setParameter("productId", productId)
                .getResultList();

        return result.isEmpty() ? null :
                BlueprintActivity.builder()
                        .blueprintId((Integer) result.get(0)[1])
                        .craftQuantity((Integer) result.get(0)[0])
                        .activityId((Integer) result.get(0)[2])
                        .build();
    }

    /**
     * Retrieves blueprint information by blueprint ID.
     *
     * @param blueprintId The type ID of the blueprint
     * @return BlueprintActivity containing blueprint details, or null if not found
     */
    @Transactional(readOnly = true)
    public BlueprintActivity getBlueprintInfoByBlueprint(Integer blueprintId) {
        String nativeQuery = "SELECT quantity, typeID, activityID " +
                "FROM industryActivityProducts " +
                "WHERE typeID = :blueprintId";

        List<Object[]> result = entityManager.createNativeQuery(nativeQuery)
                .setParameter("blueprintId", blueprintId)
                .getResultList();

        return result.isEmpty() ? null :
                BlueprintActivity.builder()
                        .blueprintId((Integer) result.get(0)[1])
                        .craftQuantity((Integer) result.get(0)[0])
                        .activityId((Integer) result.get(0)[2])
                        .build();
    }

    /**
     * Retrieves all manufacturing and reaction blueprints.
     * Filters out special edition blueprints.
     *
     * @return List of blueprints for activity ID 1 (manufacturing) and 11 (reactions)
     */
    @Transactional(readOnly = true)
    public List<Blueprint> getAllBlueprints() {
        String nativeQuery = "SELECT typeID, productTypeID, activityID " +
                "FROM industryActivityProducts " +
                "WHERE activityID IN (1, 11)";

        List<Tuple> result = entityManager.createNativeQuery(nativeQuery, Tuple.class)
                .getResultList();

        LOGGER.debug("Retrieved {} blueprints from database", result.size());

        return result.stream()
                .map(row -> Blueprint.builder()
                        .bpId((Integer) row.get("typeID"))
                        .blueprint(getTypeName((Integer) row.get("productTypeID")))
                        .activity((Integer) row.get("activityID"))
                        .build())
                .filter(name -> Objects.nonNull(name.getBlueprint()))
                .filter(name -> !name.getBlueprint().contains("Edition"))
                .toList();
    }

    /**
     * Retrieves the volume of an item type.
     *
     * @param typeId The type ID of the item
     * @return Volume in cubic meters, or null if not found
     */
    @Transactional(readOnly = true)
    public Double getVolume(Integer typeId) {
        String nativeQuery = "SELECT volume FROM invVolumes WHERE typeID = :typeId";
        List<Object> result = entityManager.createNativeQuery(nativeQuery)
                .setParameter("typeId", typeId)
                .getResultList();

        return result.isEmpty() ? null : (Double) result.get(0);
    }

    /**
     * Helper method to retrieve the name of a type by its ID.
     *
     * @param id The type ID
     * @return Type name, or null if not found
     */
    private String getTypeName(Integer id) {
        String nativeQuery = "SELECT typeName FROM invTypes WHERE typeID = :id";
        List<Object> result = entityManager.createNativeQuery(nativeQuery)
                .setParameter("id", id)
                .getResultList();

        return result.isEmpty() ? null : (String) result.get(0);
    }
}
