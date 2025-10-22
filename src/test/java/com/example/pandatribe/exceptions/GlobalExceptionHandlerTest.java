package com.example.pandatribe.exceptions;

import com.example.pandatribe.models.responses.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for GlobalExceptionHandler to verify custom exception handling.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        /**
         * Test controller to trigger various exceptions
         */
        @RestController
        @RequestMapping("/api/test")
        static class TestExceptionController {

            @GetMapping("/blueprint-not-found")
            public void throwBlueprintNotFoundException() {
                throw new BlueprintNotFoundException("TestBlueprint");
            }

            @GetMapping("/request-not-found")
            public void throwRequestNotFoundException() {
                throw new RequestNotFoundException("test-request-id");
            }

            @GetMapping("/invalid-request")
            public void throwInvalidRequestException() {
                throw new InvalidRequestException("testField", "is invalid");
            }

            @GetMapping("/external-api-error")
            public void throwExternalApiException() {
                throw new ExternalApiException("TestAPI", 503, "Service unavailable");
            }

            @GetMapping("/generic-error")
            public void throwGenericException() {
                throw new RuntimeException("Unexpected error occurred");
            }

            @PostMapping("/validation-error")
            public void throwValidationError(@RequestBody TestRequest request) {
                // Spring validation will handle this
            }

            @GetMapping("/type-mismatch/{id}")
            public void typeMismatch(@PathVariable Integer id) {
                // Will be used to test type mismatch
            }
        }

        record TestRequest(String name) {
        }

        @Bean
        public TestExceptionController testExceptionController() {
            return new TestExceptionController();
        }
    }

    @Test
    @DisplayName("Should handle BlueprintNotFoundException with 404 status")
    void shouldHandleBlueprintNotFoundException() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/test/blueprint-not-found"))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertThat(error.getStatus()).isEqualTo(404);
        assertThat(error.getErrorCode()).isEqualTo("BLUEPRINT_NOT_FOUND");
        assertThat(error.getMessage()).contains("TestBlueprint");
        assertThat(error.getTimestamp()).isNotNull();
        assertThat(error.getPath()).isEqualTo("/api/test/blueprint-not-found");
        assertThat(error.getDetails()).containsEntry("blueprintName", "TestBlueprint");
    }

    @Test
    @DisplayName("Should handle RequestNotFoundException with 404 status")
    void shouldHandleRequestNotFoundException() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/test/request-not-found"))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertThat(error.getStatus()).isEqualTo(404);
        assertThat(error.getErrorCode()).isEqualTo("REQUEST_NOT_FOUND");
        assertThat(error.getMessage()).contains("test-request-id");
        assertThat(error.getDetails()).containsEntry("requestId", "test-request-id");
    }

    @Test
    @DisplayName("Should handle InvalidRequestException with 400 status")
    void shouldHandleInvalidRequestException() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/test/invalid-request"))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertThat(error.getStatus()).isEqualTo(400);
        assertThat(error.getErrorCode()).isEqualTo("INVALID_REQUEST");
        assertThat(error.getMessage()).contains("testField");
        assertThat(error.getDetails()).containsEntry("fieldName", "testField");
    }

    @Test
    @DisplayName("Should handle ExternalApiException with 503 status")
    void shouldHandleExternalApiException() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/test/external-api-error"))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        ErrorResponse error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertThat(error.getStatus()).isEqualTo(503);
        assertThat(error.getErrorCode()).isEqualTo("EXTERNAL_API_ERROR");
        assertThat(error.getMessage()).contains("External service temporarily unavailable");
        assertThat(error.getDetails()).containsEntry("apiName", "TestAPI");
        assertThat(error.getDetails()).containsEntry("statusCode", "503");
    }

    @Test
    @DisplayName("Should handle generic exceptions with 500 status")
    void shouldHandleGenericException() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/test/generic-error"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        ErrorResponse error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertThat(error.getStatus()).isEqualTo(500);
        assertThat(error.getErrorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(error.getMessage()).contains("unexpected error occurred");
        // Should not expose internal error details to client
    }

    @Test
    @DisplayName("Should handle type mismatch exceptions with 400 status")
    void shouldHandleTypeMismatchException() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/test/type-mismatch/invalid-number"))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        assertThat(error.getStatus()).isEqualTo(400);
        assertThat(error.getErrorCode()).isEqualTo("TYPE_MISMATCH");
        assertThat(error.getMessage()).contains("Invalid value");
        assertThat(error.getDetails()).containsKey("parameter");
    }

    @Test
    @DisplayName("Error response should always include required fields")
    void errorResponseShouldIncludeRequiredFields() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/test/blueprint-not-found"))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ErrorResponse.class
        );

        // Verify all required fields are present
        assertThat(error.getTimestamp()).isNotNull();
        assertThat(error.getStatus()).isNotNull();
        assertThat(error.getError()).isNotNull();
        assertThat(error.getMessage()).isNotNull();
        assertThat(error.getPath()).isNotNull();
        assertThat(error.getErrorCode()).isNotNull();
    }

    @Test
    @DisplayName("Should return proper JSON structure for all errors")
    void shouldReturnProperJsonStructure() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/test/blueprint-not-found"))
                .andExpect(status().isNotFound())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertThat(json).contains("\"timestamp\"");
        assertThat(json).contains("\"status\"");
        assertThat(json).contains("\"error\"");
        assertThat(json).contains("\"message\"");
        assertThat(json).contains("\"path\"");
        assertThat(json).contains("\"errorCode\"");
    }
}
