package com.example.pandatribe.sdeupdater.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for converting EVE SDE JSONL files to SQL migration scripts.
 * Handles common functionality for reading JSONL and writing SQL.
 */
@Slf4j
public abstract class SdeJsonToSqlConverter {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts a JSONL file to a SQL migration script
     *
     * @param inputPath path to the JSONL file
     * @param outputPath path to write the SQL file
     * @throws IOException if file operations fail
     */
    public void convertToSql(Path inputPath, Path outputPath) throws IOException {
        log.info("Converting {} to SQL: {}", inputPath.getFileName(), outputPath.getFileName());

        try (BufferedReader reader = Files.newBufferedReader(inputPath);
             BufferedWriter writer = Files.newBufferedWriter(outputPath)) {

            // Write table drop and create statements
            writeDropStatement(writer);
            writeCreateStatement(writer);

            // Process JSONL records in batches
            String line;
            List<JsonNode> batch = new ArrayList<>();
            int batchSize = 1000;
            int totalRecords = 0;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                JsonNode record = objectMapper.readTree(line);
                batch.add(record);

                if (batch.size() >= batchSize) {
                    writeInsertStatements(writer, batch);
                    totalRecords += batch.size();
                    batch.clear();

                    if (totalRecords % 10000 == 0) {
                        log.debug("Processed {} records from {}", totalRecords, inputPath.getFileName());
                    }
                }
            }

            // Write remaining records
            if (!batch.isEmpty()) {
                writeInsertStatements(writer, batch);
                totalRecords += batch.size();
            }

            // Write indexes and constraints
            writeIndexStatements(writer);

            log.info("Converted {} records from {} to SQL", totalRecords, inputPath.getFileName());
        }
    }

    /**
     * Writes the DROP TABLE statement
     */
    protected abstract void writeDropStatement(BufferedWriter writer) throws IOException;

    /**
     * Writes the CREATE TABLE statement
     */
    protected abstract void writeCreateStatement(BufferedWriter writer) throws IOException;

    /**
     * Writes INSERT statements for a batch of records
     */
    protected abstract void writeInsertStatements(BufferedWriter writer, List<JsonNode> records) throws IOException;

    /**
     * Writes index and constraint statements
     */
    protected abstract void writeIndexStatements(BufferedWriter writer) throws IOException;

    /**
     * Escapes SQL strings to prevent injection and handle special characters
     */
    protected String escapeSql(String value) {
        if (value == null) {
            return "NULL";
        }
        return "'" + value.replace("'", "''")
                         .replace("\\", "\\\\") + "'";
    }

    /**
     * Formats a SQL NULL value
     */
    protected String sqlNull() {
        return "NULL";
    }

    /**
     * Gets a string value from JSON node or NULL
     */
    protected String getString(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return sqlNull();
        }
        return escapeSql(field.asText());
    }

    /**
     * Gets an integer value from JSON node or NULL
     */
    protected String getInt(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return sqlNull();
        }
        return String.valueOf(field.asInt());
    }

    /**
     * Gets a long value from JSON node or NULL
     */
    protected String getLong(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return sqlNull();
        }
        return String.valueOf(field.asLong());
    }

    /**
     * Gets a double value from JSON node or NULL
     */
    protected String getDouble(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return sqlNull();
        }
        return String.valueOf(field.asDouble());
    }

    /**
     * Gets a boolean value as TINYINT (1/0) or NULL
     */
    protected String getBoolean(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return sqlNull();
        }
        return field.asBoolean() ? "1" : "0";
    }
}
