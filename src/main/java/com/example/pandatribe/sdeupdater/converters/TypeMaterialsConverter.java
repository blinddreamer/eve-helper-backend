package com.example.pandatribe.sdeupdater.converters;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Converts typeMaterials.jsonl (reprocessing yields) to SQL for the invTypeMaterials table.
 * Record shape: {"_key": typeID, "materials": [{"materialTypeID": 34, "quantity": 400}, ...]}
 */
@Component
public class TypeMaterialsConverter extends SdeJsonToSqlConverter {

    @Override
    public List<String> tables() {
        return List.of("invTypeMaterials");
    }

    @Override
    protected void writeDropStatement(BufferedWriter writer) throws IOException {
        writer.write("DROP TABLE IF EXISTS invTypeMaterials_new;\n\n");
    }

    @Override
    protected void writeCreateStatement(BufferedWriter writer) throws IOException {
        writer.write("CREATE TABLE invTypeMaterials_new (\n");
        writer.write("  typeID INT NOT NULL,\n");
        writer.write("  materialTypeID INT NOT NULL,\n");
        writer.write("  quantity INT NOT NULL DEFAULT 0,\n");
        writer.write("  PRIMARY KEY (typeID, materialTypeID)\n");
        writer.write(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n\n");
    }

    @Override
    protected void writeInsertStatements(BufferedWriter writer, List<JsonNode> records) throws IOException {
        StringBuilder insert = new StringBuilder("INSERT INTO invTypeMaterials_new (typeID, materialTypeID, quantity) VALUES\n");
        boolean any = false;

        for (JsonNode record : records) {
            JsonNode materials = record.get("materials");
            if (materials == null || !materials.isArray()) {
                continue; // a few types only have randomizedMaterials; not used for reprocessing
            }
            int typeID = record.get("_key").asInt();
            for (JsonNode material : materials) {
                if (any) {
                    insert.append(",\n");
                }
                insert.append("(")
                        .append(typeID).append(", ")
                        .append(material.get("materialTypeID").asInt()).append(", ")
                        .append(material.get("quantity").asInt())
                        .append(")");
                any = true;
            }
        }

        if (any) {
            insert.append(";\n\n");
            writer.write(insert.toString());
        }
    }

    @Override
    protected void writeIndexStatements(BufferedWriter writer) throws IOException {
        // primary key covers lookups by typeID
    }
}
