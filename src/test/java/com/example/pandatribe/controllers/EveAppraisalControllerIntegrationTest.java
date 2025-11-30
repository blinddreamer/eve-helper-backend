package com.example.pandatribe.controllers;

import com.example.pandatribe.models.dbmodels.appraisal.AppraisalData;
import com.example.pandatribe.models.requests.AppraisalRequest;
import com.example.pandatribe.models.requests.AppraisalRequestEntity;
import com.example.pandatribe.models.responses.ErrorResponse;
import com.example.pandatribe.repositories.interfaces.AppraisalDataRepository;
import com.example.pandatribe.utils.EncodingUtil;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for EveAppraisalController.
 * Tests the complete request/response cycle including database operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EveAppraisalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppraisalDataRepository appraisalDataRepository;

    @Autowired
    private EncodingUtil encodingUtil;

    @Test
    @DisplayName("Should create appraisal with single item successfully")
    void shouldCreateAppraisalWithSingleItemSuccessfully() throws Exception {
        // Arrange
        AppraisalRequestEntity entity = new AppraisalRequestEntity();
        entity.setName("Tritanium");
        entity.setQuantity(1000);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity));
        request.setRegionId("10000002_60003760");
        request.setPricePercentage(100.0);
        request.setTransactionType("buy");
        request.setComment("Integration test appraisal");
        request.setSystem("Jita");

        // Act
        MvcResult result = mockMvc.perform(post("/api/v1/appraisal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String shortenedLink = result.getResponse().getContentAsString();

        // Assert
        assertThat(shortenedLink).isNotNull();
        assertThat(shortenedLink).isNotEmpty();

        // Verify data was saved to database
        UUID uuid = encodingUtil.decompressUUID(shortenedLink);
        AppraisalData savedData = appraisalDataRepository.findById(uuid).orElse(null);

        assertThat(savedData).isNotNull();
        assertThat(savedData.getTransactionType()).isEqualTo("buy");
        assertThat(savedData.getComment()).isEqualTo("Integration test appraisal");
        assertThat(savedData.getSystem()).isEqualTo("Jita");
        assertThat(savedData.getMarket()).isEqualTo("10000002_60003760");
        assertThat(savedData.getPricePercentage()).isEqualTo(100.0);
        assertThat(savedData.getAppraisalResult()).isNotNull();
        assertThat(savedData.getAppraisalResult().getAppraisals()).hasSize(1);
    }

    @Test
    @DisplayName("Should create appraisal with multiple items successfully")
    void shouldCreateAppraisalWithMultipleItemsSuccessfully() throws Exception {
        // Arrange
        AppraisalRequestEntity entity1 = new AppraisalRequestEntity();
        entity1.setName("Tritanium");
        entity1.setQuantity(1000);

        AppraisalRequestEntity entity2 = new AppraisalRequestEntity();
        entity2.setName("Pyerite");
        entity2.setQuantity(500);

        AppraisalRequestEntity entity3 = new AppraisalRequestEntity();
        entity3.setName("Mexallon");
        entity3.setQuantity(250);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity1, entity2, entity3));
        request.setRegionId("10000002_60003760");

        // Act
        MvcResult result = mockMvc.perform(post("/api/v1/appraisal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String shortenedLink = result.getResponse().getContentAsString();

        // Assert
        UUID uuid = encodingUtil.decompressUUID(shortenedLink);
        AppraisalData savedData = appraisalDataRepository.findById(uuid).orElse(null);

        assertThat(savedData).isNotNull();
        assertThat(savedData.getAppraisalResult().getAppraisals()).hasSize(3);
        assertThat(savedData.getAppraisalResult().getEstimateTotalBuy()).isNotNull();
        assertThat(savedData.getAppraisalResult().getEstimateTotalSell()).isNotNull();
        assertThat(savedData.getAppraisalResult().getEstimateTotalSplit()).isNotNull();
        assertThat(savedData.getAppraisalResult().getTotalVolume()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Should retrieve appraisal by ID successfully")
    void shouldRetrieveAppraisalByIdSuccessfully() throws Exception {
        // Arrange - Create an appraisal first
        AppraisalRequestEntity entity = new AppraisalRequestEntity();
        entity.setName("Tritanium");
        entity.setQuantity(500);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity));
        request.setRegionId("10000002_60003760");
        request.setComment("Test retrieval");

        MvcResult createResult = mockMvc.perform(post("/api/v1/appraisal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String shortenedLink = createResult.getResponse().getContentAsString();

        // Act - Retrieve the appraisal
        mockMvc.perform(get("/api/v1/appraisal/{id}", shortenedLink))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.comment").value("Test retrieval"))
                .andExpect(jsonPath("$.market").value("10000002_60003760"))
                .andExpect(jsonPath("$.appraisalResult").exists())
                .andExpect(jsonPath("$.appraisalResult.appraisals[0].item").value("Tritanium"))
                .andExpect(jsonPath("$.appraisalResult.appraisals[0].quantity").value(500))
                .andExpect(jsonPath("$.appraisalResult.estimateTotalBuy").exists())
                .andExpect(jsonPath("$.appraisalResult.estimateTotalSell").exists());
    }

    @Test
    @DisplayName("Should return error for non-existent appraisal ID")
    void shouldReturnErrorForNonExistentAppraisalId() throws Exception {
        // Arrange
        String nonExistentId = "nonexistent123abc";

        // Act & Assert
        mockMvc.perform(get("/api/v1/appraisal/{id}", nonExistentId))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Should create appraisal with default region when not specified")
    void shouldCreateAppraisalWithDefaultRegion() throws Exception {
        // Arrange
        AppraisalRequestEntity entity = new AppraisalRequestEntity();
        entity.setName("Tritanium");
        entity.setQuantity(100);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity));
        // Not setting regionId - should use default

        // Act
        MvcResult result = mockMvc.perform(post("/api/v1/appraisal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String shortenedLink = result.getResponse().getContentAsString();

        // Assert
        UUID uuid = encodingUtil.decompressUUID(shortenedLink);
        AppraisalData savedData = appraisalDataRepository.findById(uuid).orElse(null);

        assertThat(savedData).isNotNull();
        assertThat(savedData.getAppraisalResult()).isNotNull();
        assertThat(savedData.getAppraisalResult().getAppraisals()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle appraisal with sell transaction type")
    void shouldHandleAppraisalWithSellTransactionType() throws Exception {
        // Arrange
        AppraisalRequestEntity entity = new AppraisalRequestEntity();
        entity.setName("Tritanium");
        entity.setQuantity(2000);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity));
        request.setTransactionType("sell");
        request.setPricePercentage(95.0);

        // Act
        MvcResult result = mockMvc.perform(post("/api/v1/appraisal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String shortenedLink = result.getResponse().getContentAsString();

        // Assert
        UUID uuid = encodingUtil.decompressUUID(shortenedLink);
        AppraisalData savedData = appraisalDataRepository.findById(uuid).orElse(null);

        assertThat(savedData).isNotNull();
        assertThat(savedData.getTransactionType()).isEqualTo("sell");
        assertThat(savedData.getPricePercentage()).isEqualTo(95.0);
    }

    @Test
    @DisplayName("Should calculate total volumes and prices correctly")
    void shouldCalculateTotalVolumesAndPricesCorrectly() throws Exception {
        // Arrange
        AppraisalRequestEntity entity = new AppraisalRequestEntity();
        entity.setName("Tritanium");
        entity.setQuantity(1000);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity));

        // Act
        MvcResult result = mockMvc.perform(post("/api/v1/appraisal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String shortenedLink = result.getResponse().getContentAsString();

        // Assert
        UUID uuid = encodingUtil.decompressUUID(shortenedLink);
        AppraisalData savedData = appraisalDataRepository.findById(uuid).orElse(null);

        assertThat(savedData).isNotNull();
        assertThat(savedData.getAppraisalResult().getTotalVolume()).isGreaterThan(0.0);
        assertThat(savedData.getAppraisalResult().getEstimateTotalBuy()).isNotNull();
        assertThat(savedData.getAppraisalResult().getEstimateTotalSell()).isNotNull();
        assertThat(savedData.getAppraisalResult().getEstimateTotalSplit()).isNotNull();

        // Split should be approximately between buy and sell
        var buy = savedData.getAppraisalResult().getEstimateTotalBuy();
        var sell = savedData.getAppraisalResult().getEstimateTotalSell();
        var split = savedData.getAppraisalResult().getEstimateTotalSplit();

        assertThat(split).isBetween(buy, sell);
    }

    @Test
    @DisplayName("Should handle complete appraisal workflow")
    void shouldHandleCompleteAppraisalWorkflow() throws Exception {
        // Step 1: Create appraisal
        AppraisalRequestEntity entity1 = new AppraisalRequestEntity();
        entity1.setName("Tritanium");
        entity1.setQuantity(1000);

        AppraisalRequestEntity entity2 = new AppraisalRequestEntity();
        entity2.setName("Pyerite");
        entity2.setQuantity(500);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity1, entity2));
        request.setRegionId("10000002_60003760");
        request.setTransactionType("buy");
        request.setComment("Workflow test");
        request.setSystem("Jita");
        request.setPricePercentage(100.0);

        MvcResult createResult = mockMvc.perform(post("/api/v1/appraisal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String shortenedLink = createResult.getResponse().getContentAsString();

        // Step 2: Retrieve and verify
        MvcResult getResult = mockMvc.perform(get("/api/v1/appraisal/{id}", shortenedLink))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("Workflow test"))
                .andExpect(jsonPath("$.transactionType").value("buy"))
                .andExpect(jsonPath("$.system").value("Jita"))
                .andExpect(jsonPath("$.appraisalResult.appraisals").isArray())
                .andReturn();

        AppraisalData retrievedData = objectMapper.readValue(
                getResult.getResponse().getContentAsString(),
                AppraisalData.class
        );

        assertThat(retrievedData.getAppraisalResult().getAppraisals()).hasSize(2);
        assertThat(retrievedData.getCreationDate()).isNotNull();
    }

    @Test
    @DisplayName("Should persist appraisal with all optional fields")
    void shouldPersistAppraisalWithAllOptionalFields() throws Exception {
        // Arrange
        AppraisalRequestEntity entity = new AppraisalRequestEntity();
        entity.setName("Tritanium");
        entity.setQuantity(100);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity));
        request.setRegionId("10000002_60003760");
        request.setPricePercentage(98.5);
        request.setTransactionType("split");
        request.setComment("Full optional fields test");
        request.setSystem("Amarr");

        // Act
        MvcResult result = mockMvc.perform(post("/api/v1/appraisal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String shortenedLink = result.getResponse().getContentAsString();

        // Assert
        UUID uuid = encodingUtil.decompressUUID(shortenedLink);
        AppraisalData savedData = appraisalDataRepository.findById(uuid).orElse(null);

        assertThat(savedData).isNotNull();
        assertThat(savedData.getTransactionType()).isEqualTo("split");
        assertThat(savedData.getComment()).isEqualTo("Full optional fields test");
        assertThat(savedData.getSystem()).isEqualTo("Amarr");
        assertThat(savedData.getMarket()).isEqualTo("10000002_60003760");
        assertThat(savedData.getPricePercentage()).isEqualTo(98.5);
        assertThat(savedData.getCreationDate()).isNotNull();
    }
}
