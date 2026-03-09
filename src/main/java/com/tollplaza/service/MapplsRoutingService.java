package com.tollplaza.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tollplaza.exception.RoutingServiceException;
import com.tollplaza.model.Coordinates;
import com.tollplaza.model.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@Primary
@ConditionalOnProperty(name = "routing.service.provider", havingValue = "mappls", matchIfMissing = true)
public class MapplsRoutingService implements RoutingService {
    
    private static final Logger logger = LoggerFactory.getLogger(MapplsRoutingService.class);
    // Updated Mappls API endpoints for REST API Key authentication
    private static final String GEOCODING_API_URL = "https://apis.mappls.com/advancedmaps/v1/%s/geo_code";
    private static final String DIRECTIONS_API_URL = "https://apis.mappls.com/advancedmaps/v1/%s/route_adv/driving/%s;%s";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final int maxRetryAttempts;
    
    public MapplsRoutingService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${routing.service.api-key}") String apiKey,
            @Value("${routing.service.timeout:5000}") int timeout,
            @Value("${routing.service.retry.max-attempts:3}") int maxRetryAttempts) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeout))
                .setReadTimeout(Duration.ofMillis(timeout))
                .defaultHeader("Referer", "http://localhost:8080")
                .defaultHeader("User-Agent", "TollPlazaFinder/1.0")
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.maxRetryAttempts = maxRetryAttempts;
        logger.info("Initialized Mappls Routing Service");
    }
    
    @Override
    public Route getRoute(Coordinates source, Coordinates destination) {
        logger.debug("Getting route from {} to {}", source, destination);
        
        return executeWithRetry(() -> {
            String sourceCoords = String.format("%f,%f", source.getLongitude(), source.getLatitude());
            String destCoords = String.format("%f,%f", destination.getLongitude(), destination.getLatitude());
            String url = String.format(DIRECTIONS_API_URL, apiKey, sourceCoords, destCoords);
            
            try {
                String response = restTemplate.getForObject(url, String.class);
                return parseRouteResponse(response, source, destination);
            } catch (ResourceAccessException e) {
                logger.error("Timeout while calling Mappls routing service", e);
                throw new RoutingServiceException("Routing service timeout", e);
            } catch (RestClientException e) {
                logger.error("Error calling Mappls routing service", e);
                throw new RoutingServiceException("Routing service unavailable", e);
            }
        });
    }
    
    @Override
    public Coordinates getCoordinatesForPincode(String pincode) {
        logger.debug("Getting coordinates for pincode: {}", pincode);
        
        return executeWithRetry(() -> {
            String url = String.format(GEOCODING_API_URL + "?addr=%s", apiKey, pincode);
            
            try {
                String response = restTemplate.getForObject(url, String.class);
                return parseGeocodingResponse(response, pincode);
            } catch (ResourceAccessException e) {
                logger.error("Timeout while calling Mappls geocoding service for pincode: {}", pincode, e);
                throw new RoutingServiceException("Geocoding service timeout", e);
            } catch (RestClientException e) {
                logger.error("Error calling Mappls geocoding service for pincode: {}", pincode, e);
                throw new RoutingServiceException("Geocoding service unavailable", e);
            }
        });
    }
    
    private Route parseRouteResponse(String response, Coordinates source, Coordinates destination) {
        try {
            JsonNode root = objectMapper.readTree(response);
            
            // Check for error response
            if (root.has("error")) {
                String error = root.path("error").asText();
                logger.warn("Mappls API returned error: {}", error);
                throw new RoutingServiceException("Unable to find route: " + error);
            }
            
            JsonNode routes = root.path("routes");
            if (routes.isEmpty()) {
                throw new RoutingServiceException("No routes found");
            }
            
            JsonNode route = routes.get(0);
            
            // Get distance in meters and convert to km
            double distanceMeters = route.path("distance").asDouble();
            double distanceInKm = distanceMeters / 1000.0;
            
            // Extract path points from geometry
            List<Coordinates> pathPoints = new ArrayList<>();
            pathPoints.add(source);
            
            JsonNode legs = route.path("legs");
            for (JsonNode leg : legs) {
                JsonNode steps = leg.path("steps");
                for (JsonNode step : steps) {
                    JsonNode geometry = step.path("geometry");
                    if (geometry.has("coordinates")) {
                        JsonNode coords = geometry.path("coordinates");
                        for (JsonNode coord : coords) {
                            double lon = coord.get(0).asDouble();
                            double lat = coord.get(1).asDouble();
                            pathPoints.add(new Coordinates(lat, lon));
                        }
                    }
                }
            }
            
            pathPoints.add(destination);
            
            logger.info("Route found with distance: {} km and {} path points", distanceInKm, pathPoints.size());
            return new Route(source, destination, pathPoints, distanceInKm);
            
        } catch (Exception e) {
            logger.error("Error parsing Mappls route response", e);
            throw new RoutingServiceException("Failed to parse route response", e);
        }
    }
    
    private Coordinates parseGeocodingResponse(String response, String pincode) {
        try {
            JsonNode root = objectMapper.readTree(response);
            
            // Check for error
            if (root.has("error") || root.has("responseCode")) {
                String error = root.has("error") ? root.path("error").asText() : "Unknown error";
                logger.warn("Mappls Geocoding API returned error: {} for pincode: {}", error, pincode);
                throw new RoutingServiceException("Unable to geocode pincode: " + pincode + " - " + error);
            }
            
            // Mappls REST API returns results array
            JsonNode results = root.path("results");
            if (results.isEmpty() || !results.isArray() || results.size() == 0) {
                throw new RoutingServiceException("No results found for pincode: " + pincode);
            }
            
            JsonNode firstResult = results.get(0);
            double lat = firstResult.path("lat").asDouble();
            double lng = firstResult.path("lng").asDouble();
            
            // Fallback to latitude/longitude fields if lat/lng not found
            if (lat == 0.0 && lng == 0.0) {
                lat = firstResult.path("latitude").asDouble();
                lng = firstResult.path("longitude").asDouble();
            }
            
            logger.debug("Coordinates for pincode {}: ({}, {})", pincode, lat, lng);
            return new Coordinates(lat, lng);
            
        } catch (Exception e) {
            logger.error("Error parsing Mappls geocoding response for pincode: {}", pincode, e);
            throw new RoutingServiceException("Failed to parse geocoding response", e);
        }
    }
    
    /**
     * Execute operation with retry logic (exponential backoff)
     */
    private <T> T executeWithRetry(RetryableOperation<T> operation) {
        int attempt = 0;
        long delay = 1000; // Initial delay: 1 second
        
        while (attempt < maxRetryAttempts) {
            try {
                return operation.execute();
            } catch (RoutingServiceException e) {
                attempt++;
                if (attempt >= maxRetryAttempts) {
                    logger.error("Max retry attempts ({}) reached", maxRetryAttempts);
                    throw e;
                }
                
                logger.warn("Attempt {} failed, retrying in {} ms", attempt, delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RoutingServiceException("Retry interrupted", ie);
                }
                
                delay = Math.min(delay * 2, 5000); // Exponential backoff, max 5 seconds
            }
        }
        
        throw new RoutingServiceException("Failed after " + maxRetryAttempts + " attempts");
    }
    
    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute();
    }
}
