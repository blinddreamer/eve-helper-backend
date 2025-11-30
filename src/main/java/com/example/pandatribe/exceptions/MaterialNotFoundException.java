package com.example.pandatribe.exceptions;

import lombok.Getter;

/**
 * Exception thrown when a material or item type is not found in the EVE database.
 * This typically occurs when querying for invalid type IDs or materials that don't exist.
 */
@Getter
public class MaterialNotFoundException extends RuntimeException {

    private final Integer typeId;
    private final String materialName;

    /**
     * Creates a new MaterialNotFoundException with a type ID.
     *
     * @param typeId The EVE type ID that was not found
     */
    public MaterialNotFoundException(Integer typeId) {
        super(String.format("Material with type ID %d not found in EVE database", typeId));
        this.typeId = typeId;
        this.materialName = null;
    }

    /**
     * Creates a new MaterialNotFoundException with a material name.
     *
     * @param materialName The name of the material that was not found
     */
    public MaterialNotFoundException(String materialName) {
        super(String.format("Material '%s' not found in EVE database", materialName));
        this.typeId = null;
        this.materialName = materialName;
    }

    /**
     * Creates a new MaterialNotFoundException with both type ID and material name.
     *
     * @param typeId The EVE type ID that was not found
     * @param materialName The name of the material that was not found
     */
    public MaterialNotFoundException(Integer typeId, String materialName) {
        super(String.format("Material '%s' (type ID: %d) not found in EVE database", materialName, typeId));
        this.typeId = typeId;
        this.materialName = materialName;
    }
}
