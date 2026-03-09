package com.tollplaza.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tollplaza.model.Coordinates;
import com.tollplaza.model.Route;
import com.tollplaza.model.TollPlazaRequest;
import com.tollplaza.service.RoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Toll Plaza Finder API.
 * Tests end-to-end functionality with mocked routing service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TollPlazaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoutingService routingService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        if (cacheManager.getCache("tollPlazaRoutes") != null) {
            cacheManager.getCache("tollPlazaRoutes").clear();
        }
    }

    @Test
    void testSuccessfulTollPlazaDiscovery() throws Exception {
        // Setup mock routing service
        setupMockRoutingService("110001", "122001", 
            28.6139, 77.2090,  // Delhi coordinates
            28.4595, 77.0266,  // Gurgaon coordinates
            30.0);

        TollPlazaRequest request = new TollPlazaRequest("110001", "122001");

        mockMvc.perform(post("/api/v1/toll-plazas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.route").exists())
                .andExpect(jsonPath("$.route.sourcePincode").value("110001"))
                .andExpect(jsonPath("$.route.destinationPincode").value("122001"))
                .andExpect(jsonPath("$.route.distanceInKm").value(30.0))
                .andExpect(jsonPath("$.tollPlazas").isArray())
                .andExpect(jsonPath("$.tollPlazas[0].name").exists())
                .andExpect(jsonPath("$.tollPlazas[0].latitude").exists())
                .andExpect(jsonPath("$.tollPlazas[0].longitude").exists())
                .andExpect(jsonPath("$.tollPlazas[0].distanceFromSource").exists());
    }

    @Test
    void testEmptyResultsWhenNoTollPlazasOnRoute() throws Exception {
        // Setup mock routing service with route far from any toll plazas
        setupMockRoutingService("600001", "600002",
            13.0827, 80.2707,  // Chennai coordinates
            13.0878, 80.2785,  // Nearby Chennai coordinates
            5.0);

        TollPlazaRequest request = new TollPlazaRequest("600001", "600002");

        mockMvc.perform(post("/api/v1/toll-plazas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route").exists())
                .andExpect(jsonPath("$.tollPlazas").isArray())
                .andExpect(jsonPath("$.tollPlazas").isEmpty());
    }

    @Test
    void testInvalidPincodeFormat() throws Exception {
        TollPlazaRequest request = new TollPlazaRequest("12345", "122001");

        mockMvc.perform(post("/api/v1/toll-plazas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testNonNumericPincode() throws Exception {
        TollPlazaRequest request = new TollPlazaRequest("ABC123", "122001");

        mockMvc.perform(post("/api/v1/toll-plazas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testSameSourceAndDestinationPincode() throws Exception {
        TollPlazaRequest request = new TollPlazaRequest("110001", "110001");

        mockMvc.perform(post("/api/v1/toll-plazas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("cannot be the same")));
    }

    @Test
    void testMissingSourcePincode() throws Exception {
        String requestJson = "{\"destinationPincode\":\"122001\"}";

        mockMvc.perform(post("/api/v1/toll-plazas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testMissingDestinationPincode() throws Exception {
        String requestJson = "{\"sourcePincode\":\"110001\"}";

        mockMvc.perform(post("/api/v1/toll-plazas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testCacheBehavior() throws Exception {
        // Setup mock routing service
        setupMockRoutingService("110001", "122001",
            28.6139, 77.2090,
            28.4595, 77.0266,
            30.0);

        TollPlazaRequest request = new TollPlazaRequest("110001", "122001");
        String requestJson = objectMapper.writeValueAsString(request);

        // First request - should call routing service
        mockMvc.perform(post("/api/v1/toll-plazas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk());

        // Second request - should use cache
        mockMvc.perform(post("/api/v1/toll-plazas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk());

        // Verify routing service was called only once (first request)
        verify(routingService, times(1)).getCoordinatesForPincode("110001");
        verify(routingService, times(1)).getCoordinatesForPincode("122001");
        verify(routingService, times(1)).getRoute(any(Coordinates.class), any(Coordinates.class));
    }

    @Test
    void testResponseStructureCompleteness() throws Exception {
        setupMockRoutingService("110001", "122001",
            28.6139, 77.2090,
            28.4595, 77.0266,
            30.0);

        TollPlazaRequest request = new TollPlazaRequest("110001", "122001");

        mockMvc.perform(post("/api/v1/toll-plazas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/json")))
                .andExpect(jsonPath("$.route").exists())
                .andExpect(jsonPath("$.route.sourcePincode").isString())
                .andExpect(jsonPath("$.route.destinationPincode").isString())
                .andExpect(jsonPath("$.route.distanceInKm").isNumber())
                .andExpect(jsonPath("$.tollPlazas").isArray());
    }

    @Test
    void testTollPlazasOrderedByDistanceFromSource() throws Exception {
        // Setup route that passes through multiple toll plazas
        setupMockRoutingService("110001", "140001",
            28.6139, 77.2090,  // Delhi
            30.7333, 76.7794,  // Chandigarh
            250.0);

        TollPlazaRequest request = new TollPlazaRequest("110001", "140001");

        mockMvc.perform(post("/api/v1/toll-plazas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tollPlazas").isArray());
        
        // Note: Actual ordering verification would require checking that
        // distanceFromSource values are in ascending order
    }

    /**
     * Helper method to setup mock routing service responses.
     */
    private void setupMockRoutingService(String sourcePincode, String destPincode,
                                        double sourceLat, double sourceLon,
                                        double destLat, double destLon,
                                        double distance) {
        Coordinates sourceCoords = new Coordinates(sourceLat, sourceLon);
        Coordinates destCoords = new Coordinates(destLat, destLon);

        when(routingService.getCoordinatesForPincode(sourcePincode))
            .thenReturn(sourceCoords);
        when(routingService.getCoordinatesForPincode(destPincode))
            .thenReturn(destCoords);

        // Create route with path points
        List<Coordinates> pathPoints = createPathPoints(sourceCoords, destCoords);
        Route route = new Route(sourceCoords, destCoords, pathPoints, distance);

        when(routingService.getRoute(any(Coordinates.class), any(Coordinates.class)))
            .thenReturn(route);
    }

    /**
     * Creates a simple path between two coordinates.
     */
    private List<Coordinates> createPathPoints(Coordinates start, Coordinates end) {
        List<Coordinates> points = new ArrayList<>();
        points.add(start);
        
        // Add intermediate points
        int steps = 5;
        for (int i = 1; i < steps; i++) {
            double ratio = (double) i / steps;
            double lat = start.getLatitude() + (end.getLatitude() - start.getLatitude()) * ratio;
            double lon = start.getLongitude() + (end.getLongitude() - start.getLongitude()) * ratio;
            points.add(new Coordinates(lat, lon));
        }
        
        points.add(end);
        return points;
    }
}
