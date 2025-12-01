package com.example.pandatribe.sdeupdater.converters;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Converts types.jsonl to SQL for invTypes and invVolumes tables
 */
@Slf4j
@Component
public class TypesConverter extends SdeJsonToSqlConverter {

    @Override
    protected void writeDropStatement(BufferedWriter writer) throws IOException {
        writer.write("DROP TABLE IF EXISTS invTypes;\n");
        writer.write("DROP TABLE IF EXISTS invVolumes;\n\n");
    }

    @Override
    protected void writeCreateStatement(BufferedWriter writer) throws IOException {
        // invTypes table
        writer.write("CREATE TABLE invTypes (\n");
        writer.write("  typeID INT NOT NULL,\n");
        writer.write("  groupID INT,\n");
        writer.write("  typeName VARCHAR(255),\n");
        writer.write("  description TEXT,\n");
        writer.write("  mass DOUBLE,\n");
        writer.write("  volume DOUBLE,\n");
        writer.write("  capacity DOUBLE,\n");
        writer.write("  portionSize INT,\n");
        writer.write("  raceID INT,\n");
        writer.write("  basePrice DECIMAL(19,4),\n");
        writer.write("  published TINYINT(1),\n");
        writer.write("  marketGroupID INT,\n");
        writer.write("  iconID INT,\n");
        writer.write("  soundID INT,\n");
        writer.write("  graphicID INT,\n");
        writer.write("  PRIMARY KEY (typeID)\n");
        writer.write(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n\n");

        // invVolumes table
        writer.write("CREATE TABLE invVolumes (\n");
        writer.write("  typeID INT NOT NULL,\n");
        writer.write("  volume DOUBLE,\n");
        writer.write("  PRIMARY KEY (typeID)\n");
        writer.write(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n\n");
    }

    @Override
    protected void writeInsertStatements(BufferedWriter writer, List<JsonNode> records) throws IOException {
        StringBuilder typesInsert = new StringBuilder("INSERT INTO invTypes (typeID, groupID, typeName, description, mass, volume, capacity, portionSize, raceID, basePrice, published, marketGroupID, iconID, soundID, graphicID) VALUES\n");
        StringBuilder volumesInsert = new StringBuilder("INSERT INTO invVolumes (typeID, volume) VALUES\n");

        boolean hasVolumes = false;

        for (int i = 0; i < records.size(); i++) {
            JsonNode type = records.get(i);

            if (i > 0) {
                typesInsert.append(",\n");
            }

            int typeID = type.get("_key").asInt();

            // Extract name from localized names (use English)
            String typeName = sqlNull();
            JsonNode nameNode = type.get("name");
            if (nameNode != null) {
                if (nameNode.has("en")) {
                    typeName = escapeSql(nameNode.get("en").asText());
                } else if (nameNode.isTextual()) {
                    typeName = escapeSql(nameNode.asText());
                }
            }

            // Extract description (use English)
            String description = sqlNull();
            JsonNode descNode = type.get("description");
            if (descNode != null && descNode.has("en")) {
                description = escapeSql(descNode.get("en").asText());
            }

            typesInsert.append("(")
                .append(typeID).append(", ")
                .append(getInt(type, "groupID")).append(", ")
                .append(typeName).append(", ")
                .append(description).append(", ")
                .append(getDouble(type, "mass")).append(", ")
                .append(getDouble(type, "volume")).append(", ")
                .append(getDouble(type, "capacity")).append(", ")
                .append(getInt(type, "portionSize")).append(", ")
                .append(getInt(type, "raceID")).append(", ")
                .append(getDouble(type, "basePrice")).append(", ")
                .append(getBoolean(type, "published")).append(", ")
                .append(getInt(type, "marketGroupID")).append(", ")
                .append(getInt(type, "iconID")).append(", ")
                .append(getInt(type, "soundID")).append(", ")
                .append(getInt(type, "graphicID"))
                .append(")");

            // Add to volumes insert if volume exists
            JsonNode volumeNode = type.get("volume");
            if (volumeNode != null && !volumeNode.isNull()) {
                if (hasVolumes) {
                    volumesInsert.append(",\n");
                }
                volumesInsert.append("(")
                    .append(typeID).append(", ")
                    .append(volumeNode.asDouble())
                    .append(")");
                hasVolumes = true;
            }
        }

        typesInsert.append(";\n\n");
        writer.write(typesInsert.toString());

        // Only write volumes INSERT if we have data
        if (hasVolumes) {
            volumesInsert.append(";\n\n");
            writer.write(volumesInsert.toString());
        }
    }

    @Override
    protected void writeIndexStatements(BufferedWriter writer) throws IOException {
        writer.write("CREATE INDEX idx_types_groupID ON invTypes(groupID);\n");
        writer.write("CREATE INDEX idx_types_marketGroupID ON invTypes(marketGroupID);\n");
        writer.write("CREATE INDEX idx_types_published ON invTypes(published);\n");
        writer.write("CREATE INDEX idx_types_name ON invTypes(typeName);\n");
    }
}
