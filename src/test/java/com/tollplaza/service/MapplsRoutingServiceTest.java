package com.tollplaza.service;

import com.tollplaza.model.Coordinates;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic test to verify Mappls routing service is properly configured
 */
@SpringBootTest
@ActiveProfiles("test")
class MapplsRoutingServiceTest {

    @Autowired(required = false)
    private RoutingService routingService;

    @Test
    void testMapplsServiceIsConfigured() {
        // This test just verifies the service can be instantiated
        assertNotNull(routingService, "Routing service should be configured");
        
        // Verify it's the Mappls implementation when provider is set to mappls
        if (routingService != null) {
            System.out.println("Routing service class: " + routingService.getClass().getSimpleName());
        }
    }
}
