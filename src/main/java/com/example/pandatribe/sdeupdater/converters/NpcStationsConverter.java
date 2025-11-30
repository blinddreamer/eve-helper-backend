package com.example.pandatribe.sdeupdater.converters;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Converts npcStations.jsonl to SQL for staStations table
 */
@Slf4j
@Component
public class NpcStationsConverter extends SdeJsonToSqlConverter {

    @Override
    protected void writeDropStatement(BufferedWriter writer) throws IOException {
        writer.write("DROP TABLE IF EXISTS staStations;\n\n");
    }

    @Override
    protected void writeCreateStatement(BufferedWriter writer) throws IOException {
        writer.write("CREATE TABLE staStations (\n");
        writer.write("  stationID BIGINT NOT NULL,\n");
        writer.write("  stationName VARCHAR(255),\n");
        writer.write("  solarSystemID INT,\n");
        writer.write("  regionID INT,\n");
        writer.write("  stationTypeID INT,\n");
        writer.write("  PRIMARY KEY (stationID)\n");
        writer.write(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n\n");
    }

    @Override
    protected void writeInsertStatements(BufferedWriter writer, List<JsonNode> records) throws IOException {
        StringBuilder insert = new StringBuilder("INSERT INTO staStations (stationID, stationName, solarSystemID, regionID, stationTypeID) VALUES\n");

        for (int i = 0; i < records.size(); i++) {
            JsonNode station = records.get(i);

            if (i > 0) {
                insert.append(",\n");
            }

            long stationID = station.get("_key").asLong();

            String stationName = sqlNull();
            JsonNode nameNode = station.get("name");
            if (nameNode != null && nameNode.has("en")) {
                stationName = escapeSql(nameNode.get("en").asText());
            }

            insert.append("(")
                .append(stationID).append(", ")
                .append(stationName).append(", ")
                .append(getInt(station, "solarSystemID")).append(", ")
                .append(getInt(station, "regionID")).append(", ")
                .append(getInt(station, "typeID"))
                .append(")");
        }

        insert.append(";\n\n");
        writer.write(insert.toString());
    }

    @Override
    protected void writeIndexStatements(BufferedWriter writer) throws IOException {
        writer.write("CREATE INDEX idx_stations_name ON staStations(stationName);\n");
        writer.write("CREATE INDEX idx_stations_system ON staStations(solarSystemID);\n");
        writer.write("CREATE INDEX idx_stations_region ON staStations(regionID);\n");
    }
}
