package com.example.pandatribe.sdeupdater.converters;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Converts npcStations.jsonl to SQL for staStations table
 *
 * NOTE: The new SDE JSON format does NOT include station names.
 * Station names were previously in invNames table or must be fetched via ESI API.
 * The stationName field will be NULL after import and should be populated separately.
 */
@Slf4j
@Component
public class NpcStationsConverter extends SdeJsonToSqlConverter {

    @Override
    protected void writeDropStatement(BufferedWriter writer) throws IOException {
        // Backup existing station names before dropping (if table exists)
        writer.write("DROP TABLE IF EXISTS staStations_backup;\n");
        writer.write("CREATE TABLE staStations_backup (stationID BIGINT, stationName VARCHAR(255));\n\n");
        writer.write("-- Backup station names if table exists\n");
        writer.write("SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables \n");
        writer.write("  WHERE table_schema = DATABASE() AND table_name = 'staStations');\n");
        writer.write("SET @sql_backup = IF(@table_exists > 0,\n");
        writer.write("  'INSERT INTO staStations_backup SELECT stationID, stationName FROM staStations WHERE stationName IS NOT NULL',\n");
        writer.write("  'SELECT 1');\n");
        writer.write("PREPARE stmt FROM @sql_backup;\n");
        writer.write("EXECUTE stmt;\n");
        writer.write("DEALLOCATE PREPARE stmt;\n\n");
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
        // Restore station names from backup (new JSON format doesn't include names)
        writer.write("UPDATE staStations s\n");
        writer.write("INNER JOIN staStations_backup b ON s.stationID = b.stationID\n");
        writer.write("SET s.stationName = b.stationName;\n\n");
        writer.write("DROP TABLE IF EXISTS staStations_backup;\n\n");

        writer.write("CREATE INDEX idx_stations_name ON staStations(stationName);\n");
        writer.write("CREATE INDEX idx_stations_system ON staStations(solarSystemID);\n");
        writer.write("CREATE INDEX idx_stations_region ON staStations(regionID);\n");
    }
}
