package com.example.pandatribe.services;

import com.example.pandatribe.exceptions.BlueprintNotFoundException;
import com.example.pandatribe.exceptions.RequestNotFoundException;
import com.example.pandatribe.models.dbmodels.industry.BlueprintData;
import com.example.pandatribe.models.requests.BlueprintRequest;
import com.example.pandatribe.repositories.interfaces.BlueprintDataRepository;
import com.example.pandatribe.services.contracts.BlueprintService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for BlueprintService.
 * Tests business logic and exception handling.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlueprintServiceTest {

    @Autowired
    private BlueprintService blueprintService;

    @Autowired
    private BlueprintDataRepository blueprintDataRepository;

    @Test
    @DisplayName("Should throw BlueprintNotFoundException for invalid blueprint name")
    void shouldThrowExceptionForInvalidBlueprintName() {
        BlueprintRequest request = BlueprintRequest.builder()
                .blueprintName("CompletelyInvalidBlueprintNameThatDoesNotExist")
                .runs(1)
                .build();

        assertThatThrownBy(() -> blueprintService.getInitialBlueprintData(request))
                .isInstanceOf(BlueprintNotFoundException.class)
                .hasMessageContaining("CompletelyInvalidBlueprintNameThatDoesNotExist");
    }

    @Test
    @DisplayName("Should throw RequestNotFoundException for invalid request ID in update")
    void shouldThrowExceptionForInvalidRequestId() {
        BlueprintRequest request = BlueprintRequest.builder()
                .requestId("non-existent-id-12345")
                .blueprintName("Tritanium")
                .runs(1)
                .build();

        assertThatThrownBy(() -> blueprintService.updateSubMaterials(request))
                .isInstanceOf(RequestNotFoundException.class)
                .hasMessageContaining("non-existent-id-12345");
    }

    @Test
    @DisplayName("Should retrieve all EVE systems")
    void shouldRetrieveAllEveSystems() {
        var systems = blueprintService.getEveSystems();

        assertThat(systems).isNotNull();
        assertThat(systems).isNotEmpty();
        assertThat(systems.get(0).getSystemName()).isNotBlank();
    }

    @Test
    @DisplayName("Should retrieve all EVE regions")
    void shouldRetrieveAllEveRegions() {
        var regions = blueprintService.getEveRegions();

        assertThat(regions).isNotNull();
        assertThat(regions).isNotEmpty();
        assertThat(regions.get(0).getRegionId()).isNotNull();
        assertThat(regions.get(0).getRegionName()).isNotBlank();
    }

    @Test
    @DisplayName("Should retrieve featured stations")
    void shouldRetrieveFeaturedStations() {
        var stations = blueprintService.getEveStations();

        assertThat(stations).isNotNull();
        assertThat(stations).isNotEmpty();

        // Verify stations are sorted by region name
        for (int i = 0; i < stations.size() - 1; i++) {
            String current = stations.get(i).getRegionName();
            String next = stations.get(i + 1).getRegionName();
            assertThat(current).isLessThanOrEqualTo(next);
        }
    }

    @Test
    @DisplayName("Should retrieve all blueprints with complexity")
    void shouldRetrieveAllBlueprintsWithComplexity() {
        var result = blueprintService.getEveBlueprints();

        assertThat(result).isNotNull();
        assertThat(result.getBlueprints()).isNotEmpty();

        // Verify blueprints have complexity assigned
        var firstBlueprint = result.getBlueprints().get(0);
        assertThat(firstBlueprint.getBpId()).isNotNull();
        assertThat(firstBlueprint.getBlueprint()).isNotBlank();
        assertThat(firstBlueprint.getActivity()).isIn(1, 11);
    }

    @Test
    @DisplayName("Should create and update blueprint data")
    void shouldCreateAndUpdateBlueprintData() {
        // Create initial blueprint
        BlueprintRequest createRequest = BlueprintRequest.builder()
                .blueprintName("Tritanium")
                .runs(100)
                .blueprintMe(10)
                .system("Jita")
                .build();

        BlueprintData created = blueprintService.getInitialBlueprintData(createRequest);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotBlank();
        assertThat(created.getBlueprintResult()).hasSize(1);
        assertThat(created.getBlueprintResult().get(0).getName()).isEqualTo("Tritanium");

        // Update the blueprint
        BlueprintRequest updateRequest = BlueprintRequest.builder()
                .requestId(created.getId())
                .blueprintName("Tritanium")
                .runs(200)
                .build();

        BlueprintData updated = blueprintService.updateSubMaterials(updateRequest);

        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(created.getId());

        // Verify it was persisted
        Optional<BlueprintData> fromDb = blueprintDataRepository.findById(created.getId());
        assertThat(fromDb).isPresent();
    }

    @Test
    @DisplayName("Should handle mass update of materials")
    void shouldHandleMassUpdateOfMaterials() {
        // First create a blueprint
        BlueprintRequest createRequest = BlueprintRequest.builder()
                .blueprintName("Tritanium")
                .runs(100)
                .build();

        BlueprintData created = blueprintService.getInitialBlueprintData(createRequest);

        // Prepare mass update requests
        List<BlueprintRequest> massUpdateRequests = List.of(
                BlueprintRequest.builder()
                        .requestId(created.getId())
                        .blueprintName("Tritanium")
                        .runs(150)
                        .build()
        );

        BlueprintData updated = blueprintService.massUpdateMaterials(massUpdateRequests);

        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(created.getId());
    }

    @Test
    @DisplayName("Should throw exception for mass update with invalid request ID")
    void shouldThrowExceptionForMassUpdateWithInvalidId() {
        List<BlueprintRequest> requests = List.of(
                BlueprintRequest.builder()
                        .requestId("invalid-id-123")
                        .blueprintName("Tritanium")
                        .runs(100)
                        .build()
        );

        assertThatThrownBy(() -> blueprintService.massUpdateMaterials(requests))
                .isInstanceOf(RequestNotFoundException.class)
                .hasMessageContaining("invalid-id-123");
    }

    @Test
    @DisplayName("Should handle blueprint requests with all optional parameters")
    void shouldHandleBlueprintRequestsWithAllParameters() {
        BlueprintRequest request = BlueprintRequest.builder()
                .blueprintName("Tritanium")
                .runs(100)
                .blueprintMe(10)
                .buildingRig(2)
                .building(1)
                .system("Jita")
                .facilityTax(5.0)
                .regionId("10000002_60003760")
                .tier(1)
                .count(10)
                .init(true)
                .build();

        BlueprintData result = blueprintService.getInitialBlueprintData(request);

        assertThat(result).isNotNull();
        assertThat(result.getBlueprintResult()).isNotEmpty();

        var blueprint = result.getBlueprintResult().get(0);
        assertThat(blueprint.getBlueprintMaterialEfficiency()).isEqualTo(10);
        assertThat(blueprint.getRigDiscount()).isEqualTo(2);
        assertThat(blueprint.getBuildingDiscount()).isEqualTo(1);
        assertThat(blueprint.getSystem()).isEqualTo("Jita");
    }

    @Test
    @DisplayName("Should use default values when optional parameters are not provided")
    void shouldUseDefaultValuesForOptionalParameters() {
        BlueprintRequest request = BlueprintRequest.builder()
                .blueprintName("Tritanium")
                .build();

        BlueprintData result = blueprintService.getInitialBlueprintData(request);

        assertThat(result).isNotNull();
        var blueprint = result.getBlueprintResult().get(0);

        // Verify defaults are applied
        assertThat(blueprint.getSystem()).isEqualTo("Jita");
        assertThat(blueprint.getBlueprintMaterialEfficiency()).isEqualTo(0);
        assertThat(blueprint.getRigDiscount()).isEqualTo(0);
        assertThat(blueprint.getBuildingDiscount()).isEqualTo(0);
    }
}
