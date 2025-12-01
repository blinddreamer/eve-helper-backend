package com.example.pandatribe.sdeupdater.converters;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Converts blueprints.jsonl to SQL for industry activity tables
 * Creates industryActivityMaterials and industryActivityProducts tables
 */
@Slf4j
@Component
public class BlueprintsConverter extends SdeJsonToSqlConverter {

    @Override
    protected void writeDropStatement(BufferedWriter writer) throws IOException {
        writer.write("DROP TABLE IF EXISTS industryActivityMaterials;\n");
        writer.write("DROP TABLE IF EXISTS industryActivityProducts;\n");
        writer.write("DROP TABLE IF EXISTS industryActivity;\n\n");
    }

    @Override
    protected void writeCreateStatement(BufferedWriter writer) throws IOException {
        // Industry Activity Materials table
        writer.write("CREATE TABLE industryActivityMaterials (\n");
        writer.write("  typeID INT NOT NULL,\n");
        writer.write("  activityID INT NOT NULL,\n");
        writer.write("  materialTypeID INT NOT NULL,\n");
        writer.write("  quantity INT NOT NULL,\n");
        writer.write("  PRIMARY KEY (typeID, activityID, materialTypeID)\n");
        writer.write(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n\n");

        // Industry Activity Products table
        writer.write("CREATE TABLE industryActivityProducts (\n");
        writer.write("  typeID INT NOT NULL,\n");
        writer.write("  activityID INT NOT NULL,\n");
        writer.write("  productTypeID INT NOT NULL,\n");
        writer.write("  quantity INT,\n");
        writer.write("  PRIMARY KEY (typeID, activityID, productTypeID)\n");
        writer.write(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n\n");
    }

    @Override
    protected void writeInsertStatements(BufferedWriter writer, List<JsonNode> records) throws IOException {
        StringBuilder materialsInsert = new StringBuilder("INSERT INTO industryActivityMaterials (typeID, activityID, materialTypeID, quantity) VALUES\n");
        StringBuilder productsInsert = new StringBuilder("INSERT INTO industryActivityProducts (typeID, activityID, productTypeID, quantity) VALUES\n");

        boolean hasMaterials = false;
        boolean hasProducts = false;

        for (JsonNode blueprint : records) {
            int blueprintTypeID = blueprint.get("blueprintTypeID").asInt();
            JsonNode activities = blueprint.get("activities");

            if (activities == null) {
                continue;
            }

            // Process each activity type - using traditional iterator instead of lambda
            var activityIterator = activities.fields();
            while (activityIterator.hasNext()) {
                var entry = activityIterator.next();
                String activityName = entry.getKey();
                JsonNode activity = entry.getValue();

                // Map activity names to IDs
                int activityID = getActivityId(activityName);

                try {
                    // Process materials
                    JsonNode materials = activity.get("materials");
                    if (materials != null && materials.isArray()) {
                        for (JsonNode material : materials) {
                            if (hasMaterials) {
                                materialsInsert.append(",\n");
                            }
                            materialsInsert.append("(")
                                .append(blueprintTypeID).append(", ")
                                .append(activityID).append(", ")
                                .append(material.get("typeID").asInt()).append(", ")
                                .append(material.get("quantity").asInt())
                                .append(")");
                            hasMaterials = true;
                        }
                    }

                    // Process products
                    JsonNode products = activity.get("products");
                    if (products != null && products.isArray()) {
                        for (JsonNode product : products) {
                            if (hasProducts) {
                                productsInsert.append(",\n");
                            }
                            productsInsert.append("(")
                                .append(blueprintTypeID).append(", ")
                                .append(activityID).append(", ")
                                .append(product.get("typeID").asInt()).append(", ")
                                .append(product.has("quantity") ? product.get("quantity").asInt() : 1)
                                .append(")");
                            hasProducts = true;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error processing blueprint {}: {}", blueprintTypeID, e.getMessage());
                }
            }
        }

        if (hasMaterials) {
            materialsInsert.append(";\n\n");
            writer.write(materialsInsert.toString());
        }

        if (hasProducts) {
            productsInsert.append(";\n\n");
            writer.write(productsInsert.toString());
        }
    }

    @Override
    protected void writeIndexStatements(BufferedWriter writer) throws IOException {
        writer.write("CREATE INDEX idx_materials_typeID ON industryActivityMaterials(materialTypeID);\n");
        writer.write("CREATE INDEX idx_products_typeID ON industryActivityProducts(productTypeID);\n");
        writer.write("CREATE INDEX idx_materials_activity ON industryActivityMaterials(activityID);\n");
        writer.write("CREATE INDEX idx_products_activity ON industryActivityProducts(activityID);\n");
    }

    /**
     * Maps activity names to numeric IDs
     * Based on EVE SDE conventions
     */
    private int getActivityId(String activityName) {
        return switch (activityName) {
            case "manufacturing" -> 1;
            case "research_time" -> 3;
            case "research_material" -> 4;
            case "copying" -> 5;
            case "invention" -> 8;
            case "reaction" -> 11;
            default -> 0;
        };
    }
}
