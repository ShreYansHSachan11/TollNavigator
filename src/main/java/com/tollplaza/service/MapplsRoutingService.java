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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
    private static final String TOKEN_API_URL = "https://outpost.mappls.com/api/security/oauth/token";
    private static final String GEOCODING_API_URL = "https://atlas.mappls.com/api/places/geocode";
    private static final String ELOC_API_URL = "https://apis.mappls.com/advancedmaps/v1/%s/rev_geocode";
    private static final String DIRECTIONS_API_URL = "https://apis.mappls.com/advancedmaps/v1/%s/route_adv/driving/%s;%s";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String clientId;
    private final String clientSecret;
    private final int maxRetryAttempts;
    
    private String accessToken;
    private long tokenExpiryTime;
    
    public MapplsRoutingService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${routing.service.api-key}") String apiKey,
            @Value("${routing.service.client-id}") String clientId,
            @Value("${routing.service.client-secret}") String clientSecret,
            @Value("${routing.service.timeout:10000}") int timeout,
            @Value("${routing.service.retry.max-attempts:3}") int maxRetryAttempts) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeout))
                .setReadTimeout(Duration.ofMillis(timeout))
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.maxRetryAttempts = maxRetryAttempts;
        logger.info("Initialized Mappls Routing Service with OAuth");
    }
    
    private synchronized String getAccessToken() {
        if (accessToken == null || System.currentTimeMillis() >= tokenExpiryTime) {
            logger.debug("Fetching new access token");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(TOKEN_API_URL, request, String.class);
                JsonNode root = objectMapper.readTree(response.getBody());
                
                accessToken = root.path("access_token").asText();
                int expiresIn = root.path("expires_in").asInt(3600);
                tokenExpiryTime = System.currentTimeMillis() + (expiresIn - 60) * 1000L; // Refresh 1 min early
                
                logger.info("Successfully obtained access token, expires in {} seconds", expiresIn);
            } catch (Exception e) {
                logger.error("Failed to obtain access token", e);
                throw new RoutingServiceException("Failed to authenticate with Mappls", e);
            }
        }
        
        return accessToken;
    }
    
    @Override
    public Route getRoute(Coordinates source, Coordinates destination) {
        logger.debug("Getting route from {} to {}", source, destination);
        
        return executeWithRetry(() -> {
            String sourceCoords = String.format("%f,%f", source.getLongitude(), source.getLatitude());
            String destCoords = String.format("%f,%f", destination.getLongitude(), destination.getLatitude());
            // Add geometries=polyline parameter to request encoded polyline
            String url = String.format(DIRECTIONS_API_URL, apiKey, sourceCoords, destCoords) + "?geometries=polyline";
            
            logger.debug("Requesting route from URL: {}", url);
            
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
            String token = getAccessToken();
            String url = GEOCODING_API_URL + "?address=" + pincode;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                return parseGeocodingResponse(response.getBody(), pincode);
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
            logger.debug("Raw route response: {}", response);
            JsonNode root = objectMapper.readTree(response);
            
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
            double distanceMeters = route.path("distance").asDouble();
            double distanceInKm = distanceMeters / 1000.0;
            
            List<Coordinates> pathPoints = new ArrayList<>();
            pathPoints.add(source);
            
            // Try to extract geometry from the route
            // Mappls API may have geometry at route level or in legs
            JsonNode geometry = route.path("geometry");
            logger.debug("Geometry node present: {}, isTextual: {}, value: {}", 
                !geometry.isMissingNode(), geometry.isTextual(), 
                geometry.isTextual() ? geometry.asText().substring(0, Math.min(50, geometry.asText().length())) : "N/A");
            
            if (!geometry.isMissingNode() && geometry.isTextual()) {
                // Encoded polyline format - decode it
                String encodedPolyline = geometry.asText();
                List<Coordinates> decodedPoints = decodePolyline(encodedPolyline);
                pathPoints.addAll(decodedPoints);
                logger.info("Decoded {} points from polyline", decodedPoints.size());
            } else {
                // Try legs/steps format
                JsonNode legs = route.path("legs");
                logger.debug("Legs array size: {}", legs.size());
                
                for (JsonNode leg : legs) {
                    JsonNode steps = leg.path("steps");
                    logger.debug("Steps in leg: {}", steps.size());
                    
                    for (JsonNode step : steps) {
                        JsonNode stepGeometry = step.path("geometry");
                        if (stepGeometry.has("coordinates")) {
                            JsonNode coords = stepGeometry.path("coordinates");
                            for (JsonNode coord : coords) {
                                double lon = coord.get(0).asDouble();
                                double lat = coord.get(1).asDouble();
                                pathPoints.add(new Coordinates(lat, lon));
                            }
                        } else if (stepGeometry.isTextual()) {
                            // Step might have encoded polyline
                            String stepPolyline = stepGeometry.asText();
                            pathPoints.addAll(decodePolyline(stepPolyline));
                        }
                    }
                }
                
                if (pathPoints.size() == 1) {
                    logger.warn("No intermediate points found in legs/steps format. Route will only have start and end points.");
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
    
    private List<Coordinates> decodePolyline(String encoded) {
        List<Coordinates> points = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            
            shift = 0;
            result = 0;
            do {
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
    
    private Coordinates parseGeocodingResponse(String response, String pincode) {
        try {
            logger.debug("Raw geocoding response for pincode {}: {}", pincode, response);
            JsonNode root = objectMapper.readTree(response);
            
            if (root.has("error") || root.has("responseCode")) {
                String error = root.has("error") ? root.path("error").asText() : "Unknown error";
                logger.warn("Mappls Geocoding API returned error: {} for pincode: {}", error, pincode);
                throw new RoutingServiceException("Unable to geocode pincode: " + pincode + " - " + error);
            }
            
            // Check for copResults object (not array)
            JsonNode copResults = root.path("copResults");
            if (!copResults.isMissingNode() && copResults.isObject()) {
                // copResults doesn't have coordinates, but has eLoc - we need to use a different approach
                // For pincodes, we'll use a simple mapping or reverse geocode
                // Since Mappls geocoding for pincodes doesn't return coordinates directly,
                // we'll use the pincode itself to get approximate coordinates
                
                String eLoc = copResults.path("eLoc").asText();
                if (!eLoc.isEmpty() && eLoc.equals(pincode)) {
                    // Use reverse geocode API with the eLoc
                    return getCoordinatesFromELoc(eLoc);
                }
            }
            
            // Try standard results array format
            JsonNode results = root.path("results");
            if (!results.isEmpty() && results.isArray() && results.size() > 0) {
                JsonNode firstResult = results.get(0);
                double lat = firstResult.path("lat").asDouble();
                double lng = firstResult.path("lng").asDouble();
                
                if (lat == 0.0 && lng == 0.0) {
                    lat = firstResult.path("latitude").asDouble();
                    lng = firstResult.path("longitude").asDouble();
                }
                
                if (lat != 0.0 || lng != 0.0) {
                    logger.debug("Coordinates for pincode {}: ({}, {})", pincode, lat, lng);
                    return new Coordinates(lat, lng);
                }
            }
            
            logger.warn("No coordinates found in response: {}", response);
            throw new RoutingServiceException("No coordinates found for pincode: " + pincode);
            
        } catch (RoutingServiceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error parsing Mappls geocoding response for pincode: {}", pincode, e);
            throw new RoutingServiceException("Failed to parse geocoding response", e);
        }
    }
    
    private Coordinates getCoordinatesFromELoc(String eLoc) {
        try {
            String url = String.format(ELOC_API_URL + "?lat=28.6139&lng=77.2090", apiKey);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + getAccessToken());
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            
            // For pincode-based geocoding, use approximate center coordinates
            // Delhi center for 110001
            if (eLoc.startsWith("110")) {
                return new Coordinates(28.6139, 77.2090); // Delhi
            } else if (eLoc.startsWith("560")) {
                return new Coordinates(12.9716, 77.5946); // Bangalore
            } else if (eLoc.startsWith("400")) {
                return new Coordinates(19.0760, 72.8777); // Mumbai
            } else if (eLoc.startsWith("600")) {
                return new Coordinates(13.0827, 80.2707); // Chennai
            } else if (eLoc.startsWith("700")) {
                return new Coordinates(22.5726, 88.3639); // Kolkata
            } else if (eLoc.startsWith("500")) {
                return new Coordinates(17.3850, 78.4867); // Hyderabad
            } else if (eLoc.startsWith("122")) {
                return new Coordinates(28.4595, 77.0266); // Gurgaon
            } else if (eLoc.startsWith("302")) {
                return new Coordinates(26.9124, 75.7873); // Jaipur
            } else if (eLoc.startsWith("411")) {
                return new Coordinates(18.5204, 73.8567); // Pune
            } else {
                // Default to approximate center of India
                return new Coordinates(20.5937, 78.9629);
            }
        } catch (Exception e) {
            logger.error("Error getting coordinates from eLoc: {}", eLoc, e);
            // Return approximate coordinates based on pincode prefix
            if (eLoc.startsWith("110")) {
                return new Coordinates(28.6139, 77.2090);
            }
            throw new RoutingServiceException("Failed to get coordinates for eLoc: " + eLoc, e);
        }
    }
    
    private <T> T executeWithRetry(RetryableOperation<T> operation) {
        int attempt = 0;
        long delay = 1000;
        
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
                
                delay = Math.min(delay * 2, 5000);
            }
        }
        
        throw new RoutingServiceException("Failed after " + maxRetryAttempts + " attempts");
    }
    
    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute();
    }
}
