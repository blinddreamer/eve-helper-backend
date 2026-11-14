package com.example.pandatribe.sdeupdater.converters;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Converts npcStations.jsonl to SQL for the staStations table.
 *
 * The JSON SDE has no station names and no regionID:
 * - names are copied from the current live staStations table (new stations get NULL)
 * - regionID is joined from mapSolarSystems_new, so the mapSolarSystems script must run first
 *   (Flyway runs repeatables in description order: "mapSolarSystems" < "npcStations").
 */
@Slf4j
@Component
public class NpcStationsConverter extends SdeJsonToSqlConverter {

    @Override
    public List<String> tables() {
        return List.of("staStations");
    }

    @Override
    protected void writeDropStatement(BufferedWriter writer) throws IOException {
        writer.write("DROP TABLE IF EXISTS staStations_new;\n\n");
    }

    @Override
    protected void writeCreateStatement(BufferedWriter writer) throws IOException {
        writer.write("CREATE TABLE staStations_new (\n");
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
        StringBuilder insert = new StringBuilder("INSERT INTO staStations_new (stationID, stationName, solarSystemID, regionID, stationTypeID) VALUES\n");

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
                .append(sqlNull()).append(", ")  // regionID - populated from mapSolarSystems_new below
                .append(getInt(station, "typeID"))
                .append(")");
        }

        insert.append(";\n\n");
        writer.write(insert.toString());
    }

    @Override
    protected void writeIndexStatements(BufferedWriter writer) throws IOException {
        // Keep names we already have (the live table must exist for the join)
        writer.write("CREATE TABLE IF NOT EXISTS staStations LIKE staStations_new;\n");
        writer.write("UPDATE staStations_new s\n");
        writer.write("INNER JOIN staStations old ON s.stationID = old.stationID\n");
        writer.write("SET s.stationName = old.stationName\n");
        writer.write("WHERE s.stationName IS NULL AND old.stationName IS NOT NULL;\n\n");

        writer.write("UPDATE staStations_new s\n");
        writer.write("INNER JOIN mapSolarSystems_new sys ON s.solarSystemID = sys.solarSystemID\n");
        writer.write("SET s.regionID = sys.regionID;\n\n");

        writer.write("CREATE INDEX idx_stations_name ON staStations_new(stationName);\n");
        writer.write("CREATE INDEX idx_stations_system ON staStations_new(solarSystemID);\n");
        writer.write("CREATE INDEX idx_stations_region ON staStations_new(regionID);\n");
    }
}
