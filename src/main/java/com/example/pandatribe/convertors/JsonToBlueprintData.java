package com.example.pandatribe.convertors;

import com.example.pandatribe.models.results.BlueprintResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Converter(autoApply = true)
public class JsonToBlueprintData implements AttributeConverter<List<BlueprintResult>, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonToBlueprintData.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<BlueprintResult> blueprintResult) {
        try {
            if (blueprintResult == null || blueprintResult.isEmpty()) {
                return ""; // Store empty string instead of "{}"
            }
            return mapper.writeValueAsString(blueprintResult);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error converting BlueprintResult to JSON: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Cannot convert BlueprintResult to JSON", e);
        }
    }

    @Override
    public List<BlueprintResult> convertToEntityAttribute(String blueprintResult) {
        try {
            if (blueprintResult == null || blueprintResult.trim().isEmpty()) {
                return new ArrayList<>(); // Return empty list instead of null
            }
            return mapper.readValue(blueprintResult, new TypeReference<List<BlueprintResult>>() {});
        } catch (JsonProcessingException e) {
            LOGGER.error("Error converting JSON to BlueprintResult: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Cannot convert JSON to BlueprintResult", e);
        }
    }
}
