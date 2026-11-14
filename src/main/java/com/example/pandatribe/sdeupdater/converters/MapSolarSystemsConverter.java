package com.example.pandatribe.sdeupdater.converters;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Converts mapSolarSystems.jsonl to SQL for mapSolarSystems table
 */
@Slf4j
@Component
public class MapSolarSystemsConverter extends SdeJsonToSqlConverter {

    @Override
    public List<String> tables() {
        return List.of("mapSolarSystems");
    }

    @Override
    protected void writeDropStatement(BufferedWriter writer) throws IOException {
        writer.write("DROP TABLE IF EXISTS mapSolarSystems_new;\n\n");
    }

    @Override
    protected void writeCreateStatement(BufferedWriter writer) throws IOException {
        writer.write("CREATE TABLE mapSolarSystems_new (\n");
        writer.write("  solarSystemID INT NOT NULL,\n");
        writer.write("  solarSystemName VARCHAR(255),\n");
        writer.write("  regionID INT,\n");
        writer.write("  constellationID INT,\n");
        writer.write("  security DOUBLE,\n");
        writer.write("  securityClass VARCHAR(10),\n");
        writer.write("  PRIMARY KEY (solarSystemID)\n");
        writer.write(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n\n");
    }

    @Override
    protected void writeInsertStatements(BufferedWriter writer, List<JsonNode> records) throws IOException {
        StringBuilder insert = new StringBuilder("INSERT INTO mapSolarSystems_new (solarSystemID, solarSystemName, regionID, constellationID, security, securityClass) VALUES\n");

        for (int i = 0; i < records.size(); i++) {
            JsonNode system = records.get(i);

            if (i > 0) {
                insert.append(",\n");
            }

            int systemID = system.get("_key").asInt();

            String systemName = sqlNull();
            JsonNode nameNode = system.get("name");
            if (nameNode != null && nameNode.has("en")) {
                systemName = escapeSql(nameNode.get("en").asText());
            }

            insert.append("(")
                .append(systemID).append(", ")
                .append(systemName).append(", ")
                .append(getInt(system, "regionID")).append(", ")
                .append(getInt(system, "constellationID")).append(", ")
                .append(getDouble(system, "securityStatus")).append(", ")
                .append(getString(system, "securityClass"))
                .append(")");
        }

        insert.append(";\n\n");
        writer.write(insert.toString());
    }

    @Override
    protected void writeIndexStatements(BufferedWriter writer) throws IOException {
        writer.write("CREATE INDEX idx_systems_name ON mapSolarSystems_new(solarSystemName);\n");
        writer.write("CREATE INDEX idx_systems_region ON mapSolarSystems_new(regionID);\n");
        writer.write("CREATE INDEX idx_systems_constellation ON mapSolarSystems_new(constellationID);\n");
        writer.write("CREATE INDEX idx_systems_security ON mapSolarSystems_new(security);\n");
    }
}
