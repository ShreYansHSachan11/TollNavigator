package com.tollplaza.service;

import com.tollplaza.exception.RoutingServiceException;
import com.tollplaza.model.Coordinates;
import com.tollplaza.model.Route;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GoogleMapsRoutingServiceTest {
    
    private GoogleMapsRoutingService routingService;
    private RestTemplate restTemplate;
    
    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.setConnectTimeout(any())).thenReturn(builder);
        when(builder.setReadTimeout(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restTemplate);
        
        routingService = new GoogleMapsRoutingService(builder, "test-api-key", 5000, 3);
    }
    
    @Test
    void testGetRoute_Success() {
        String mockResponse = """
            {
                "status": "OK",
                "routes": [{
                    "legs": [{
                        "distance": {"value": 2100500},
                        "steps": [{
                            "polyline": {"points": "_p~iF~ps|U_ulLnnqC_mqNvxq`@"}
                        }]
                    }]
                }]
            }
            """;
        
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);
        
        Coordinates source = new Coordinates(28.7041, 77.1025);
        Coordinates destination = new Coordinates(12.9716, 77.5946);
        
        Route route = routingService.getRoute(source, destination);
        
        assertNotNull(route);
        assertEquals(2100.5, route.getDistanceInKm(), 0.01);
        assertNotNull(route.getPathPoints());
        assertTrue(route.getPathPoints().size() > 0);
    }
    
    @Test
    void testGetRoute_NoRoutesFound() {
        String mockResponse = """
            {
                "status": "OK",
                "routes": []
            }
            """;
        
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);
        
        Coordinates source = new Coordinates(28.7041, 77.1025);
        Coordinates destination = new Coordinates(12.9716, 77.5946);
        
        assertThrows(RoutingServiceException.class, () -> {
            routingService.getRoute(source, destination);
        });
    }
    
    @Test
    void testGetRoute_ServiceUnavailable() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenThrow(new RestClientException("Service unavailable"));
        
        Coordinates source = new Coordinates(28.7041, 77.1025);
        Coordinates destination = new Coordinates(12.9716, 77.5946);
        
        assertThrows(RoutingServiceException.class, () -> {
            routingService.getRoute(source, destination);
        });
    }
    
    @Test
    void testGetCoordinatesForPincode_Success() {
        String mockResponse = """
            {
                "status": "OK",
                "results": [{
                    "geometry": {
                        "location": {
                            "lat": 28.7041,
                            "lng": 77.1025
                        }
                    }
                }]
            }
            """;
        
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);
        
        Coordinates coords = routingService.getCoordinatesForPincode("110001");
        
        assertNotNull(coords);
        assertEquals(28.7041, coords.getLatitude(), 0.0001);
        assertEquals(77.1025, coords.getLongitude(), 0.0001);
    }
    
    @Test
    void testGetCoordinatesForPincode_InvalidPincode() {
        String mockResponse = """
            {
                "status": "ZERO_RESULTS",
                "results": []
            }
            """;
        
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);
        
        assertThrows(RoutingServiceException.class, () -> {
            routingService.getCoordinatesForPincode("999999");
        });
    }
}
