package com.example.pandatribe.sdeupdater.converters;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Converts planetSchematics.jsonl to SQL for planetSchematics and planetSchematicsTypeMap tables
 */
@Slf4j
@Component
public class PlanetSchematicsConverter extends SdeJsonToSqlConverter {

    @Override
    protected void writeDropStatement(BufferedWriter writer) throws IOException {
        writer.write("DROP TABLE IF EXISTS planetSchematicsTypeMap;\n");
        writer.write("DROP TABLE IF EXISTS planetSchematics;\n\n");
    }

    @Override
    protected void writeCreateStatement(BufferedWriter writer) throws IOException {
        writer.write("CREATE TABLE planetSchematics (\n");
        writer.write("  schematicID INT NOT NULL,\n");
        writer.write("  schematicName VARCHAR(255),\n");
        writer.write("  cycleTime INT,\n");
        writer.write("  PRIMARY KEY (schematicID)\n");
        writer.write(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n\n");

        writer.write("CREATE TABLE planetSchematicsTypeMap (\n");
        writer.write("  schematicID INT NOT NULL,\n");
        writer.write("  typeID INT NOT NULL,\n");
        writer.write("  quantity INT,\n");
        writer.write("  isInput TINYINT(1),\n");
        writer.write("  PRIMARY KEY (schematicID, typeID)\n");
        writer.write(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n\n");
    }

    @Override
    protected void writeInsertStatements(BufferedWriter writer, List<JsonNode> records) throws IOException {
        StringBuilder schematicsInsert = new StringBuilder("INSERT INTO planetSchematics (schematicID, schematicName, cycleTime) VALUES\n");
        StringBuilder typeMapInsert = new StringBuilder("INSERT INTO planetSchematicsTypeMap (schematicID, typeID, quantity, isInput) VALUES\n");

        boolean hasTypeMaps = false;

        for (int i = 0; i < records.size(); i++) {
            JsonNode schematic = records.get(i);

            if (i > 0) {
                schematicsInsert.append(",\n");
            }

            int schematicID = schematic.get("_key").asInt();

            String schematicName = sqlNull();
            JsonNode nameNode = schematic.get("name");
            if (nameNode != null && nameNode.has("en")) {
                schematicName = escapeSql(nameNode.get("en").asText());
            }

            schematicsInsert.append("(")
                .append(schematicID).append(", ")
                .append(schematicName).append(", ")
                .append(getInt(schematic, "cycleTime"))
                .append(")");

            // Process types (materials and products)
            JsonNode types = schematic.get("types");
            if (types != null && types.isArray()) {
                for (JsonNode type : types) {
                    if (hasTypeMaps) {
                        typeMapInsert.append(",\n");
                    }
                    typeMapInsert.append("(")
                        .append(schematicID).append(", ")
                        .append(type.get("_key").asInt()).append(", ")
                        .append(getInt(type, "quantity")).append(", ")
                        .append(getBoolean(type, "isInput"))
                        .append(")");
                    hasTypeMaps = true;
                }
            }
        }

        schematicsInsert.append(";\n\n");
        writer.write(schematicsInsert.toString());

        if (hasTypeMaps) {
            typeMapInsert.append(";\n\n");
            writer.write(typeMapInsert.toString());
        }
    }

    @Override
    protected void writeIndexStatements(BufferedWriter writer) throws IOException {
        writer.write("CREATE INDEX idx_schematics_name ON planetSchematics(schematicName);\n");
        writer.write("CREATE INDEX idx_typemap_typeID ON planetSchematicsTypeMap(typeID);\n");
        writer.write("CREATE INDEX idx_typemap_isInput ON planetSchematicsTypeMap(isInput);\n");
    }
}
