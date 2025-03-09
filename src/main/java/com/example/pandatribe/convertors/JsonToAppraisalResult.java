package com.example.pandatribe.convertors;

import com.example.pandatribe.models.results.AppraisalResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter(autoApply = true)
public class JsonToAppraisalResult implements AttributeConverter<AppraisalResult, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonToAppraisalResult.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(AppraisalResult appraisalResult) {
        try {
            if (appraisalResult == null) {
                return null; // Use null instead of empty string
            }
            return mapper.writeValueAsString(appraisalResult);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error converting appraisalResult to JSON: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Cannot convert appraisalResult to JSON", e);
        }
    }

    @Override
    public AppraisalResult convertToEntityAttribute(String appraisalResult) {
        try {
            if (appraisalResult == null || appraisalResult.trim().isEmpty()) {
                return AppraisalResult.builder().build(); // Ensure BlueprintResult has a no-arg constructor
            }
            return mapper.readValue(appraisalResult, AppraisalResult.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error converting JSON to appraisalResult: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Cannot convert JSON to appraisalResult", e);
        }
    }
}
