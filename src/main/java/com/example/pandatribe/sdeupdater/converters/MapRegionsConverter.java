package com.example.pandatribe.sdeupdater.converters;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Converts mapRegions.jsonl to SQL for mapRegions table
 */
@Slf4j
@Component
public class MapRegionsConverter extends SdeJsonToSqlConverter {

    @Override
    protected void writeDropStatement(BufferedWriter writer) throws IOException {
        writer.write("DROP TABLE IF EXISTS mapRegions;\n\n");
    }

    @Override
    protected void writeCreateStatement(BufferedWriter writer) throws IOException {
        writer.write("CREATE TABLE mapRegions (\n");
        writer.write("  regionID INT NOT NULL,\n");
        writer.write("  regionName VARCHAR(255),\n");
        writer.write("  x DOUBLE,\n");
        writer.write("  y DOUBLE,\n");
        writer.write("  z DOUBLE,\n");
        writer.write("  factionID INT,\n");
        writer.write("  nebulaID INT,\n");
        writer.write("  wormholeClassID INT,\n");
        writer.write("  PRIMARY KEY (regionID)\n");
        writer.write(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n\n");
    }

    @Override
    protected void writeInsertStatements(BufferedWriter writer, List<JsonNode> records) throws IOException {
        StringBuilder insert = new StringBuilder("INSERT INTO mapRegions (regionID, regionName, x, y, z, factionID, nebulaID, wormholeClassID) VALUES\n");

        for (int i = 0; i < records.size(); i++) {
            JsonNode region = records.get(i);

            if (i > 0) {
                insert.append(",\n");
            }

            int regionID = region.get("_key").asInt();

            // Extract name from localized names (use English)
            String regionName = sqlNull();
            JsonNode nameNode = region.get("name");
            if (nameNode != null && nameNode.has("en")) {
                regionName = escapeSql(nameNode.get("en").asText());
            }

            // Extract position coordinates
            String x = sqlNull(), y = sqlNull(), z = sqlNull();
            JsonNode position = region.get("position");
            if (position != null) {
                x = getDouble(position, "x");
                y = getDouble(position, "y");
                z = getDouble(position, "z");
            }

            insert.append("(")
                .append(regionID).append(", ")
                .append(regionName).append(", ")
                .append(x).append(", ")
                .append(y).append(", ")
                .append(z).append(", ")
                .append(getInt(region, "factionID")).append(", ")
                .append(getInt(region, "nebulaID")).append(", ")
                .append(getInt(region, "wormholeClassID"))
                .append(")");
        }

        insert.append(";\n\n");
        writer.write(insert.toString());
    }

    @Override
    protected void writeIndexStatements(BufferedWriter writer) throws IOException {
        writer.write("CREATE INDEX idx_regions_name ON mapRegions(regionName);\n");
        writer.write("CREATE INDEX idx_regions_faction ON mapRegions(factionID);\n");
    }
}
