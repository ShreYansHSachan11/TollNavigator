package com.tollplaza.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
public class OpenApiConfiguration {
    
    @Bean
    public OpenAPI tollPlazaFinderOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Toll Plaza Finder API")
                        .description("REST API service to identify toll plazas located between two Indian pincodes. " +
                                   "The system integrates with external routing services to determine the route path, " +
                                   "matches toll plazas from a CSV data source, and returns detailed information about " +
                                   "each toll plaza along the journey.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Toll Plaza Finder Team")
                                .email("support@tollplazafinder.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server"),
                        new Server()
                                .url("https://api.tollplazafinder.com")
                                .description("Production server")
                ));
    }
}
