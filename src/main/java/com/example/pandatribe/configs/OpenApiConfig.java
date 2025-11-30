package com.example.pandatribe.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for EVE Helper API documentation.
 * Provides interactive API documentation at /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI eveHelperOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:" + serverPort);
        localServer.setDescription("Local Development Server");

        Contact contact = new Contact();
        contact.setName("EVE Helper Team");
        contact.setEmail("support@eve-helper.com");

        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("EVE Helper Backend API")
                .version("3.0.0")
                .description("""
                    **EVE Helper Backend API** - A comprehensive EVE Online industry helper tool.

                    This API provides endpoints for:
                    - **Blueprints**: Manufacturing and reaction blueprint calculations
                    - **Materials**: Material requirements with industry bonuses
                    - **Market Data**: Real-time market prices from EVE ESI
                    - **Appraisals**: Item value calculations
                    - **Planetary Interaction**: PI schematic calculations
                    - **Universe**: System, region, and station information
                    - **Authentication**: EVE Online OAuth2 integration

                    ## Features
                    - Industry tax calculations
                    - Material efficiency bonuses
                    - Structure rig bonuses
                    - Recursive blueprint pricing
                    - Market order tracking
                    - Real-time EVE ESI integration

                    ## Recent Updates (Phase 3)
                    - ✅ Improved exception handling
                    - ✅ Material calculation refactoring
                    - ✅ Enhanced caching with TTL
                    - ✅ API documentation with OpenAPI
                    - ✅ Input validation

                    ## Getting Started
                    1. Explore endpoints below
                    2. Try out API calls with the interactive UI
                    3. Check response schemas
                    4. Review error responses

                    **Note:** This API requires an active EVE Online ESI connection.
                    """)
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
