package com.example.pandatribe.controllers;

import com.example.pandatribe.models.dbmodels.industry.BlueprintData;
import com.example.pandatribe.models.requests.BlueprintRequest;
import com.example.pandatribe.models.responses.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Blueprint Controller.
 * Tests the complete request/response cycle including exception handling.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EveBlueprintControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should retrieve systems successfully")
    void shouldRetrieveSystemsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/systems"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should retrieve regions successfully")
    void shouldRetrieveRegionsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/regions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should retrieve stations successfully")
    void shouldRetrieveStationsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should retrieve blueprints successfully")
    void shouldRetrieveBlueprintsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/blueprints"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.blueprints").isArray());
    }

    @Test
    @DisplayName("Should return 404 for invalid blueprint name")
    void shouldReturn404ForInvalidBlueprintName() throws Exception {
        BlueprintRequest request = BlueprintRequest.builder()
                .blueprintName("InvalidBlueprintNameThatDoesNotExist123")
                .runs(1)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertThat(errorResponse.getErrorCode()).isEqualTo("BLUEPRINT_NOT_FOUND");
        assertThat(errorResponse.getMessage()).contains("InvalidBlueprintNameThatDoesNotExist123");
        assertThat(errorResponse.getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("Should return 404 for non-existent request ID in update")
    void shouldReturn404ForNonExistentRequestId() throws Exception {
        BlueprintRequest request = BlueprintRequest.builder()
                .requestId("non-existent-request-id-12345")
                .blueprintName("Tritanium")
                .runs(1)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/update-type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertThat(errorResponse.getErrorCode()).isEqualTo("REQUEST_NOT_FOUND");
        assertThat(errorResponse.getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("Should return 400 for empty mass update request")
    void shouldReturn400ForEmptyMassUpdateRequest() throws Exception {
        List<BlueprintRequest> emptyList = List.of();

        MvcResult result = mockMvc.perform(post("/api/v1/mass-update-type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyList)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertThat(errorResponse.getErrorCode()).isEqualTo("INVALID_REQUEST");
        assertThat(errorResponse.getMessage()).contains("cannot be null or empty");
        assertThat(errorResponse.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should handle complete blueprint flow: create and update")
    void shouldHandleCompleteBlueprintFlow() throws Exception {
        // Step 1: Create a blueprint (assuming "Tritanium" exists in test data)
        BlueprintRequest createRequest = BlueprintRequest.builder()
                .blueprintName("Tritanium")
                .runs(100)
                .blueprintMe(10)
                .system("Jita")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        BlueprintData createdBlueprint = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                BlueprintData.class
        );

        assertThat(createdBlueprint.getId()).isNotNull();
        assertThat(createdBlueprint.getBlueprintResult()).isNotEmpty();

        // Step 2: Update the blueprint
        BlueprintRequest updateRequest = BlueprintRequest.builder()
                .requestId(createdBlueprint.getId())
                .blueprintName("Tritanium")
                .runs(200)
                .build();

        mockMvc.perform(post("/api/v1/update-type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(createdBlueprint.getId()));
    }
}
