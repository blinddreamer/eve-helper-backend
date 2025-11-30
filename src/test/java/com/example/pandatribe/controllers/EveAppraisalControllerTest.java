package com.example.pandatribe.controllers;

import com.example.pandatribe.logging.JsonLogger;
import com.example.pandatribe.models.dbmodels.appraisal.AppraisalData;
import com.example.pandatribe.models.requests.AppraisalRequest;
import com.example.pandatribe.models.requests.AppraisalRequestEntity;
import com.example.pandatribe.models.results.AppraisalResult;
import com.example.pandatribe.models.results.AppraisalResultEntity;
import com.example.pandatribe.services.contracts.AppraisalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for EveAppraisalController.
 * Tests controller logic with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class EveAppraisalControllerTest {

    @Mock
    private JsonLogger jsonLogger;

    @Mock
    private AppraisalService appraisalService;

    @InjectMocks
    private EveAppraisalController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /appraisal - Should create appraisal and return shortened link")
    void shouldCreateAppraisalAndReturnShortenedLink() throws Exception {
        // Arrange
        AppraisalRequestEntity entity = new AppraisalRequestEntity();
        entity.setName("Tritanium");
        entity.setQuantity(1000);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity));
        request.setRegionId("10000002_60003760");
        request.setPricePercentage(100.0);
        request.setTransactionType("buy");
        request.setComment("Test appraisal");
        request.setSystem("Jita");

        String expectedShortenedLink = "abc123xyz";

        when(appraisalService.generateAppraisalResult(any(AppraisalRequest.class)))
                .thenReturn(expectedShortenedLink);

        // Act & Assert
        mockMvc.perform(post("/api/v1/appraisal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedShortenedLink));

        // Verify
        verify(appraisalService, times(1)).generateAppraisalResult(any(AppraisalRequest.class));
        verify(jsonLogger, times(1)).println(any(AppraisalRequest.class));
    }

    @Test
    @DisplayName("POST /appraisal - Should handle multiple items in appraisal")
    void shouldHandleMultipleItemsInAppraisal() throws Exception {
        // Arrange
        AppraisalRequestEntity entity1 = new AppraisalRequestEntity();
        entity1.setName("Tritanium");
        entity1.setQuantity(1000);

        AppraisalRequestEntity entity2 = new AppraisalRequestEntity();
        entity2.setName("Pyerite");
        entity2.setQuantity(500);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity1, entity2));
        request.setRegionId("10000002_60003760");

        String expectedShortenedLink = "multi123";

        when(appraisalService.generateAppraisalResult(any(AppraisalRequest.class)))
                .thenReturn(expectedShortenedLink);

        // Act & Assert
        mockMvc.perform(post("/api/v1/appraisal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedShortenedLink));

        verify(appraisalService, times(1)).generateAppraisalResult(any(AppraisalRequest.class));
    }

    @Test
    @DisplayName("GET /appraisal/{id} - Should retrieve appraisal by ID successfully")
    void shouldRetrieveAppraisalByIdSuccessfully() throws Exception {
        // Arrange
        String appraisalId = "abc123xyz";
        UUID uuid = UUID.randomUUID();

        AppraisalResultEntity resultEntity = AppraisalResultEntity.builder()
                .item("Tritanium")
                .quantity(1000)
                .buyOrderPrice(BigDecimal.valueOf(5.50))
                .sellOrderPrice(BigDecimal.valueOf(6.00))
                .splitPrice(BigDecimal.valueOf(5.75))
                .volume(0.01)
                .buyOrdersCount(100L)
                .sellOrdersCount(50L)
                .icon("https://images.evetech.net/types/34/icon?size=32")
                .build();

        AppraisalResult appraisalResult = AppraisalResult.builder()
                .appraisals(List.of(resultEntity))
                .regionId("10000002_60003760")
                .estimateTotalBuy(BigDecimal.valueOf(5500))
                .estimateTotalSell(BigDecimal.valueOf(6000))
                .estimateTotalSplit(BigDecimal.valueOf(5750))
                .totalVolume(10.0)
                .build();

        AppraisalData appraisalData = AppraisalData.builder()
                .id(uuid)
                .appraisalResult(appraisalResult)
                .creationDate(new Date())
                .transactionType("buy")
                .comment("Test comment")
                .market("10000002_60003760")
                .pricePercentage(100.0)
                .system("Jita")
                .build();

        when(appraisalService.getAppraisalResult(eq(appraisalId)))
                .thenReturn(appraisalData);

        // Act & Assert
        mockMvc.perform(get("/api/v1/appraisal/{id}", appraisalId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(uuid.toString()))
                .andExpect(jsonPath("$.transactionType").value("buy"))
                .andExpect(jsonPath("$.comment").value("Test comment"))
                .andExpect(jsonPath("$.market").value("10000002_60003760"))
                .andExpect(jsonPath("$.pricePercentage").value(100.0))
                .andExpect(jsonPath("$.system").value("Jita"))
                .andExpect(jsonPath("$.appraisalResult.appraisals[0].item").value("Tritanium"))
                .andExpect(jsonPath("$.appraisalResult.appraisals[0].quantity").value(1000));

        // Verify
        verify(appraisalService, times(1)).getAppraisalResult(eq(appraisalId));
        verify(jsonLogger, times(1)).println(any(AppraisalData.class));
    }

    @Test
    @DisplayName("GET /appraisal/{id} - Should handle appraisal not found")
    void shouldHandleAppraisalNotFound() throws Exception {
        // Arrange
        String appraisalId = "nonexistent123";

        when(appraisalService.getAppraisalResult(eq(appraisalId)))
                .thenThrow(new IllegalArgumentException("Appraisal data not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/appraisal/{id}", appraisalId))
                .andExpect(status().is5xxServerError());

        verify(appraisalService, times(1)).getAppraisalResult(eq(appraisalId));
    }

    @Test
    @DisplayName("POST /appraisal - Should handle request with minimal fields")
    void shouldHandleRequestWithMinimalFields() throws Exception {
        // Arrange
        AppraisalRequestEntity entity = new AppraisalRequestEntity();
        entity.setName("Tritanium");
        entity.setQuantity(100);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity));

        String expectedShortenedLink = "minimal123";

        when(appraisalService.generateAppraisalResult(any(AppraisalRequest.class)))
                .thenReturn(expectedShortenedLink);

        // Act & Assert
        mockMvc.perform(post("/api/v1/appraisal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedShortenedLink));

        verify(appraisalService, times(1)).generateAppraisalResult(any(AppraisalRequest.class));
    }

    @Test
    @DisplayName("POST /appraisal - Should handle empty appraisal list")
    void shouldHandleEmptyAppraisalList() throws Exception {
        // Arrange
        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(new ArrayList<>());

        when(appraisalService.generateAppraisalResult(any(AppraisalRequest.class)))
                .thenThrow(new IllegalArgumentException("Appraisal list cannot be empty"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/appraisal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());

        verify(appraisalService, times(1)).generateAppraisalResult(any(AppraisalRequest.class));
    }

    @Test
    @DisplayName("POST /appraisal - Should handle all optional fields")
    void shouldHandleAllOptionalFields() throws Exception {
        // Arrange
        AppraisalRequestEntity entity = new AppraisalRequestEntity();
        entity.setName("Tritanium");
        entity.setQuantity(1000);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity));
        request.setRegionId("10000002_60003760");
        request.setPricePercentage(95.5);
        request.setTransactionType("sell");
        request.setComment("Full test appraisal");
        request.setSystem("Amarr");

        String expectedShortenedLink = "full123";

        when(appraisalService.generateAppraisalResult(any(AppraisalRequest.class)))
                .thenReturn(expectedShortenedLink);

        // Act & Assert
        mockMvc.perform(post("/api/v1/appraisal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedShortenedLink));

        verify(appraisalService, times(1)).generateAppraisalResult(any(AppraisalRequest.class));
    }
}
