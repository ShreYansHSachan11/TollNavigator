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
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "routing.service.provider", havingValue = "google-maps")
public class GoogleMapsRoutingService implements RoutingService {
    
    private static final Logger logger = LoggerFactory.getLogger(GoogleMapsRoutingService.class);
    private static final String GEOCODING_API_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    private static final String DIRECTIONS_API_URL = "https://maps.googleapis.com/maps/api/directions/json";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final int maxRetryAttempts;
    
    public GoogleMapsRoutingService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${routing.service.api-key}") String apiKey,
            @Value("${routing.service.timeout:5000}") int timeout,
            @Value("${routing.service.retry.max-attempts:3}") int maxRetryAttempts) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeout))
                .setReadTimeout(Duration.ofMillis(timeout))
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.maxRetryAttempts = maxRetryAttempts;
    }
    
    @Override
    public Route getRoute(Coordinates source, Coordinates destination) {
        logger.debug("Getting route from {} to {}", source, destination);
        
        return executeWithRetry(() -> {
            String url = String.format("%s?origin=%f,%f&destination=%f,%f&key=%s",
                    DIRECTIONS_API_URL,
                    source.getLatitude(), source.getLongitude(),
                    destination.getLatitude(), destination.getLongitude(),
                    apiKey);
            
            try {
                String response = restTemplate.getForObject(url, String.class);
                return parseRouteResponse(response, source, destination);
            } catch (ResourceAccessException e) {
                logger.error("Timeout while calling routing service", e);
                throw new RoutingServiceException("Routing service timeout", e);
            } catch (RestClientException e) {
                logger.error("Error calling routing service", e);
                throw new RoutingServiceException("Routing service unavailable", e);
            }
        });
    }
    
    @Override
    public Coordinates getCoordinatesForPincode(String pincode) {
        logger.debug("Getting coordinates for pincode: {}", pincode);
        
        return executeWithRetry(() -> {
            String url = String.format("%s?address=%s,India&key=%s",
                    GEOCODING_API_URL, pincode, apiKey);
            
            try {
                String response = restTemplate.getForObject(url, String.class);
                return parseGeocodingResponse(response, pincode);
            } catch (ResourceAccessException e) {
                logger.error("Timeout while calling geocoding service for pincode: {}", pincode, e);
                throw new RoutingServiceException("Geocoding service timeout", e);
            } catch (RestClientException e) {
                logger.error("Error calling geocoding service for pincode: {}", pincode, e);
                throw new RoutingServiceException("Geocoding service unavailable", e);
            }
        });
    }
    
    private Route parseRouteResponse(String response, Coordinates source, Coordinates destination) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText();
            
            if (!"OK".equals(status)) {
                logger.warn("Routing API returned status: {}", status);
                throw new RoutingServiceException("Unable to find route: " + status);
            }
            
            JsonNode routes = root.path("routes");
            if (routes.isEmpty()) {
                throw new RoutingServiceException("No routes found");
            }
            
            JsonNode route = routes.get(0);
            JsonNode legs = route.path("legs");
            
            // Calculate total distance
            double totalDistanceMeters = 0;
            for (JsonNode leg : legs) {
                totalDistanceMeters += leg.path("distance").path("value").asDouble();
            }
            double distanceInKm = totalDistanceMeters / 1000.0;
            
            // Extract path points from polyline
            List<Coordinates> pathPoints = new ArrayList<>();
            pathPoints.add(source);
            
            for (JsonNode leg : legs) {
                JsonNode steps = leg.path("steps");
                for (JsonNode step : steps) {
                    String polyline = step.path("polyline").path("points").asText();
                    pathPoints.addAll(decodePolyline(polyline));
                }
            }
            
            pathPoints.add(destination);
            
            logger.info("Route found with distance: {} km and {} path points", distanceInKm, pathPoints.size());
            return new Route(source, destination, pathPoints, distanceInKm);
            
        } catch (Exception e) {
            logger.error("Error parsing route response", e);
            throw new RoutingServiceException("Failed to parse route response", e);
        }
    }
    
    private Coordinates parseGeocodingResponse(String response, String pincode) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText();
            
            if (!"OK".equals(status)) {
                logger.warn("Geocoding API returned status: {} for pincode: {}", status, pincode);
                throw new RoutingServiceException("Unable to geocode pincode: " + pincode);
            }
            
            JsonNode results = root.path("results");
            if (results.isEmpty()) {
                throw new RoutingServiceException("No results found for pincode: " + pincode);
            }
            
            JsonNode location = results.get(0).path("geometry").path("location");
            double lat = location.path("lat").asDouble();
            double lng = location.path("lng").asDouble();
            
            logger.debug("Coordinates for pincode {}: ({}, {})", pincode, lat, lng);
            return new Coordinates(lat, lng);
            
        } catch (Exception e) {
            logger.error("Error parsing geocoding response for pincode: {}", pincode, e);
            throw new RoutingServiceException("Failed to parse geocoding response", e);
        }
    }
    
    /**
     * Decode Google Maps polyline encoding
     * Algorithm from: https://developers.google.com/maps/documentation/utilities/polylinealgorithm
     */
    private List<Coordinates> decodePolyline(String encoded) {
        List<Coordinates> points = new ArrayList<>();
        int index = 0;
        int len = encoded.length();
        int lat = 0;
        int lng = 0;
        
        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                if (index >= len) {
                    throw new RoutingServiceException("Invalid polyline encoding: unexpected end of string");
                }
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            
            shift = 0;
            result = 0;
            do {
                if (index >= len) {
                    throw new RoutingServiceException("Invalid polyline encoding: unexpected end of string");
                }
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            
            points.add(new Coordinates(lat / 1E5, lng / 1E5));
        }
        
        return points;
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
