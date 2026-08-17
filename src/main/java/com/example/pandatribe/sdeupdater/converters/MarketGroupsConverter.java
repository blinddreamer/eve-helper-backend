package com.example.pandatribe.sdeupdater.converters;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Converts marketGroups.jsonl to SQL for the invMarketGroups table.
 * This is CCP's curated in-game market tree (what the in-game Market browser shows),
 * distinct from invTypes.groupID / invGroups which is the raw inventory categorization.
 */
@Slf4j
@Component
public class MarketGroupsConverter extends SdeJsonToSqlConverter {

    @Override
    protected void writeDropStatement(BufferedWriter writer) throws IOException {
        writer.write("DROP TABLE IF EXISTS invMarketGroups;\n\n");
    }

    @Override
    protected void writeCreateStatement(BufferedWriter writer) throws IOException {
        writer.write("CREATE TABLE invMarketGroups (\n");
        writer.write("  marketGroupID INT NOT NULL,\n");
        writer.write("  parentGroupID INT,\n");
        writer.write("  marketGroupName VARCHAR(255),\n");
        writer.write("  description TEXT,\n");
        writer.write("  iconID INT,\n");
        writer.write("  hasTypes TINYINT(1),\n");
        writer.write("  PRIMARY KEY (marketGroupID)\n");
        writer.write(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n\n");
    }

    @Override
    protected void writeInsertStatements(BufferedWriter writer, List<JsonNode> records) throws IOException {
        StringBuilder insert = new StringBuilder(
                "INSERT INTO invMarketGroups (marketGroupID, parentGroupID, marketGroupName, description, iconID, hasTypes) VALUES\n");

        for (int i = 0; i < records.size(); i++) {
            JsonNode group = records.get(i);

            if (i > 0) {
                insert.append(",\n");
            }

            int marketGroupID = group.get("_key").asInt();

            String name = sqlNull();
            JsonNode nameNode = group.get("name");
            if (nameNode != null) {
                if (nameNode.has("en")) {
                    name = escapeSql(nameNode.get("en").asText());
                } else if (nameNode.isTextual()) {
                    name = escapeSql(nameNode.asText());
                }
            }

            String description = sqlNull();
            JsonNode descNode = group.get("description");
            if (descNode != null && descNode.has("en")) {
                description = escapeSql(descNode.get("en").asText());
            }

            insert.append("(")
                    .append(marketGroupID).append(", ")
                    .append(getInt(group, "parentGroupID")).append(", ")
                    .append(name).append(", ")
                    .append(description).append(", ")
                    .append(getInt(group, "iconID")).append(", ")
                    .append(getBoolean(group, "hasTypes"))
                    .append(")");
        }

        insert.append(";\n\n");
        writer.write(insert.toString());
    }

    @Override
    protected void writeIndexStatements(BufferedWriter writer) throws IOException {
        writer.write("CREATE INDEX idx_marketgroups_parentGroupID ON invMarketGroups(parentGroupID);\n");
    }
}
