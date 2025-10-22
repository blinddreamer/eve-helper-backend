package com.example.pandatribe.services;

import com.example.pandatribe.models.dbmodels.appraisal.AppraisalData;
import com.example.pandatribe.models.requests.AppraisalRequest;
import com.example.pandatribe.models.requests.AppraisalRequestEntity;
import com.example.pandatribe.repositories.BlueprintQueryRepository;
import com.example.pandatribe.repositories.interfaces.AppraisalDataRepository;
import com.example.pandatribe.repositories.interfaces.EveTypesRepository;
import com.example.pandatribe.services.contracts.MarketService;
import com.example.pandatribe.utils.EncodingUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for AppraisalService.
 * Tests business logic and data persistence.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppraisalServiceTest {

    @Autowired
    private AppraisalServiceImpl appraisalService;

    @Autowired
    private AppraisalDataRepository appraisalDataRepository;

    @Autowired
    private EveTypesRepository eveTypesRepository;

    @Autowired
    private BlueprintQueryRepository blueprintQueryRepository;

    @Autowired
    private MarketService marketService;

    @Autowired
    private EncodingUtil encodingUtil;

    @Test
    @DisplayName("Should generate appraisal result and return shortened link")
    void shouldGenerateAppraisalResultAndReturnShortenedLink() {
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

        // Act
        String shortenedLink = appraisalService.generateAppraisalResult(request);

        // Assert
        assertThat(shortenedLink).isNotNull().isNotEmpty();

        // Verify data was persisted
        UUID uuid = encodingUtil.decompressUUID(shortenedLink);
        Optional<AppraisalData> savedData = appraisalDataRepository.findById(uuid);

        assertThat(savedData).isPresent();
        assertThat(savedData.get().getTransactionType()).isEqualTo("buy");
        assertThat(savedData.get().getComment()).isEqualTo("Test appraisal");
        assertThat(savedData.get().getSystem()).isEqualTo("Jita");
    }

    @Test
    @DisplayName("Should handle multiple items in appraisal")
    void shouldHandleMultipleItemsInAppraisal() {
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
        String shortenedLink = appraisalService.generateAppraisalResult(request);

        // Assert
        UUID uuid = encodingUtil.decompressUUID(shortenedLink);
        Optional<AppraisalData> savedData = appraisalDataRepository.findById(uuid);

        assertThat(savedData).isPresent();
        assertThat(savedData.get().getAppraisalResult()).isNotNull();
        assertThat(savedData.get().getAppraisalResult().getAppraisals()).hasSize(3);
        assertThat(savedData.get().getAppraisalResult().getEstimateTotalBuy()).isNotNull();
        assertThat(savedData.get().getAppraisalResult().getEstimateTotalSell()).isNotNull();
        assertThat(savedData.get().getAppraisalResult().getTotalVolume()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Should retrieve appraisal result by shortened link")
    void shouldRetrieveAppraisalResultByShortenedLink() {
        // Arrange - Create an appraisal first
        AppraisalRequestEntity entity = new AppraisalRequestEntity();
        entity.setName("Tritanium");
        entity.setQuantity(500);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity));
        request.setComment("Retrieval test");

        String shortenedLink = appraisalService.generateAppraisalResult(request);

        // Act
        AppraisalData result = appraisalService.getAppraisalResult(shortenedLink);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getComment()).isEqualTo("Retrieval test");
        assertThat(result.getAppraisalResult()).isNotNull();
        assertThat(result.getAppraisalResult().getAppraisals()).hasSize(1);
        assertThat(result.getAppraisalResult().getAppraisals().get(0).getItem()).isEqualTo("Tritanium");
        assertThat(result.getAppraisalResult().getAppraisals().get(0).getQuantity()).isEqualTo(500);
    }

    @Test
    @DisplayName("Should throw exception for non-existent appraisal ID")
    void shouldThrowExceptionForNonExistentAppraisalId() {
        // Arrange
        String invalidId = "invalidLink123";

        // Act & Assert
        assertThatThrownBy(() -> appraisalService.getAppraisalResult(invalidId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Appraisal data not found");
    }

    @Test
    @DisplayName("Should use default region ID when not specified")
    void shouldUseDefaultRegionIdWhenNotSpecified() {
        // Arrange
        AppraisalRequestEntity entity = new AppraisalRequestEntity();
        entity.setName("Tritanium");
        entity.setQuantity(100);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity));
        // Not setting regionId

        // Act
        String shortenedLink = appraisalService.generateAppraisalResult(request);

        // Assert
        UUID uuid = encodingUtil.decompressUUID(shortenedLink);
        Optional<AppraisalData> savedData = appraisalDataRepository.findById(uuid);

        assertThat(savedData).isPresent();
        assertThat(savedData.get().getAppraisalResult()).isNotNull();
        assertThat(savedData.get().getAppraisalResult().getAppraisals()).isNotEmpty();
    }

    @Test
    @DisplayName("Should calculate buy, sell, and split prices correctly")
    void shouldCalculatePricesCorrectly() {
        // Arrange
        AppraisalRequestEntity entity = new AppraisalRequestEntity();
        entity.setName("Tritanium");
        entity.setQuantity(1000);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity));

        // Act
        String shortenedLink = appraisalService.generateAppraisalResult(request);

        // Assert
        UUID uuid = encodingUtil.decompressUUID(shortenedLink);
        Optional<AppraisalData> savedData = appraisalDataRepository.findById(uuid);

        assertThat(savedData).isPresent();
        var appraisal = savedData.get().getAppraisalResult().getAppraisals().get(0);

        assertThat(appraisal.getBuyOrderPrice()).isNotNull();
        assertThat(appraisal.getSellOrderPrice()).isNotNull();
        assertThat(appraisal.getSplitPrice()).isNotNull();

        // Split should be between buy and sell
        assertThat(appraisal.getSplitPrice())
                .isBetween(appraisal.getBuyOrderPrice(), appraisal.getSellOrderPrice());

        // Check order counts
        assertThat(appraisal.getBuyOrdersCount()).isGreaterThanOrEqualTo(0L);
        assertThat(appraisal.getSellOrdersCount()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("Should calculate total estimates correctly for multiple items")
    void shouldCalculateTotalEstimatesCorrectly() {
        // Arrange
        AppraisalRequestEntity entity1 = new AppraisalRequestEntity();
        entity1.setName("Tritanium");
        entity1.setQuantity(1000);

        AppraisalRequestEntity entity2 = new AppraisalRequestEntity();
        entity2.setName("Pyerite");
        entity2.setQuantity(500);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity1, entity2));

        // Act
        String shortenedLink = appraisalService.generateAppraisalResult(request);

        // Assert
        UUID uuid = encodingUtil.decompressUUID(shortenedLink);
        Optional<AppraisalData> savedData = appraisalDataRepository.findById(uuid);

        assertThat(savedData).isPresent();
        var result = savedData.get().getAppraisalResult();

        // Calculate expected totals manually
        BigDecimal expectedBuy = result.getAppraisals().stream()
                .map(a -> a.getBuyOrderPrice().multiply(BigDecimal.valueOf(a.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedSell = result.getAppraisals().stream()
                .map(a -> a.getSellOrderPrice().multiply(BigDecimal.valueOf(a.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(result.getEstimateTotalBuy()).isEqualByComparingTo(expectedBuy);
        assertThat(result.getEstimateTotalSell()).isEqualByComparingTo(expectedSell);
        assertThat(result.getEstimateTotalSplit()).isNotNull();
    }

    @Test
    @DisplayName("Should persist all optional fields correctly")
    void shouldPersistAllOptionalFieldsCorrectly() {
        // Arrange
        AppraisalRequestEntity entity = new AppraisalRequestEntity();
        entity.setName("Tritanium");
        entity.setQuantity(100);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity));
        request.setRegionId("10000002_60003760");
        request.setPricePercentage(95.5);
        request.setTransactionType("sell");
        request.setComment("Full optional test");
        request.setSystem("Amarr");

        // Act
        String shortenedLink = appraisalService.generateAppraisalResult(request);

        // Assert
        UUID uuid = encodingUtil.decompressUUID(shortenedLink);
        Optional<AppraisalData> savedData = appraisalDataRepository.findById(uuid);

        assertThat(savedData).isPresent();
        AppraisalData data = savedData.get();

        assertThat(data.getTransactionType()).isEqualTo("sell");
        assertThat(data.getComment()).isEqualTo("Full optional test");
        assertThat(data.getSystem()).isEqualTo("Amarr");
        assertThat(data.getMarket()).isEqualTo("10000002_60003760");
        assertThat(data.getPricePercentage()).isEqualTo(95.5);
        assertThat(data.getCreationDate()).isNotNull();
    }

    @Test
    @DisplayName("Should include icon links for items")
    void shouldIncludeIconLinksForItems() {
        // Arrange
        AppraisalRequestEntity entity = new AppraisalRequestEntity();
        entity.setName("Tritanium");
        entity.setQuantity(100);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity));

        // Act
        String shortenedLink = appraisalService.generateAppraisalResult(request);

        // Assert
        UUID uuid = encodingUtil.decompressUUID(shortenedLink);
        Optional<AppraisalData> savedData = appraisalDataRepository.findById(uuid);

        assertThat(savedData).isPresent();
        var appraisal = savedData.get().getAppraisalResult().getAppraisals().get(0);

        assertThat(appraisal.getIcon()).isNotNull();
        assertThat(appraisal.getIcon()).contains("https://");
        assertThat(appraisal.getIcon()).contains("types");
        assertThat(appraisal.getIcon()).contains("32");
    }

    @Test
    @DisplayName("Should calculate total volume correctly")
    void shouldCalculateTotalVolumeCorrectly() {
        // Arrange
        AppraisalRequestEntity entity1 = new AppraisalRequestEntity();
        entity1.setName("Tritanium");
        entity1.setQuantity(1000);

        AppraisalRequestEntity entity2 = new AppraisalRequestEntity();
        entity2.setName("Pyerite");
        entity2.setQuantity(500);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity1, entity2));

        // Act
        String shortenedLink = appraisalService.generateAppraisalResult(request);

        // Assert
        UUID uuid = encodingUtil.decompressUUID(shortenedLink);
        Optional<AppraisalData> savedData = appraisalDataRepository.findById(uuid);

        assertThat(savedData).isPresent();
        var result = savedData.get().getAppraisalResult();

        // Calculate expected volume manually
        double expectedVolume = result.getAppraisals().stream()
                .mapToDouble(a -> a.getVolume() * a.getQuantity())
                .sum();

        assertThat(result.getTotalVolume()).isEqualTo(expectedVolume);
        assertThat(result.getTotalVolume()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Should handle UUID compression and decompression")
    void shouldHandleUuidCompressionAndDecompression() {
        // Arrange
        AppraisalRequestEntity entity = new AppraisalRequestEntity();
        entity.setName("Tritanium");
        entity.setQuantity(100);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity));

        // Act
        String shortenedLink = appraisalService.generateAppraisalResult(request);
        UUID decompressed = encodingUtil.decompressUUID(shortenedLink);

        // Assert
        assertThat(shortenedLink).isNotNull();
        assertThat(decompressed).isNotNull();

        // Verify we can retrieve with the shortened link
        AppraisalData retrieved = appraisalService.getAppraisalResult(shortenedLink);
        assertThat(retrieved.getId()).isEqualTo(decompressed);
    }

    @Test
    @DisplayName("Should persist appraisal result as JSON in database")
    void shouldPersistAppraisalResultAsJson() {
        // Arrange
        AppraisalRequestEntity entity = new AppraisalRequestEntity();
        entity.setName("Tritanium");
        entity.setQuantity(100);

        AppraisalRequest request = new AppraisalRequest();
        request.setAppraisalRequestEntityList(List.of(entity));
        request.setRegionId("10000002_60003760");

        // Act
        String shortenedLink = appraisalService.generateAppraisalResult(request);

        // Assert - Retrieve directly from repository
        UUID uuid = encodingUtil.decompressUUID(shortenedLink);
        Optional<AppraisalData> savedData = appraisalDataRepository.findById(uuid);

        assertThat(savedData).isPresent();
        assertThat(savedData.get().getAppraisalResult()).isNotNull();
        assertThat(savedData.get().getAppraisalResult().getRegionId()).isEqualTo("10000002_60003760");
        assertThat(savedData.get().getAppraisalResult().getAppraisals()).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle complete workflow: create and retrieve")
    void shouldHandleCompleteWorkflow() {
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

        String shortenedLink = appraisalService.generateAppraisalResult(request);

        // Step 2: Retrieve
        AppraisalData retrieved = appraisalService.getAppraisalResult(shortenedLink);

        // Step 3: Verify
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getComment()).isEqualTo("Workflow test");
        assertThat(retrieved.getTransactionType()).isEqualTo("buy");
        assertThat(retrieved.getSystem()).isEqualTo("Jita");
        assertThat(retrieved.getMarket()).isEqualTo("10000002_60003760");
        assertThat(retrieved.getPricePercentage()).isEqualTo(100.0);
        assertThat(retrieved.getAppraisalResult().getAppraisals()).hasSize(2);
        assertThat(retrieved.getCreationDate()).isNotNull();
    }
}