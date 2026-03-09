package com.tollplaza.service;

import com.tollplaza.model.Coordinates;
import com.tollplaza.model.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock implementation of RoutingService for testing and demonstration purposes.
 * Returns predefined routes and coordinates for common Indian pincodes.
 */
@Service
@Primary
@ConditionalOnProperty(name = "routing.service.provider", havingValue = "mock")
public class MockRoutingService implements RoutingService {
    
    private static final Logger logger = LoggerFactory.getLogger(MockRoutingService.class);
    
    // Mock pincode to coordinates mapping for major Indian cities
    private static final Map<String, Coordinates> PINCODE_COORDINATES = new HashMap<>();
    
    static {
        // Delhi
        PINCODE_COORDINATES.put("110001", new Coordinates(28.6139, 77.2090));
        // Gurgaon
        PINCODE_COORDINATES.put("122001", new Coordinates(28.4595, 77.0266));
        // Bangalore
        PINCODE_COORDINATES.put("560001", new Coordinates(12.9716, 77.5946));
        // Mumbai
        PINCODE_COORDINATES.put("400001", new Coordinates(18.9388, 72.8354));
        // Pune
        PINCODE_COORDINATES.put("411001", new Coordinates(18.5204, 73.8567));
        // Hyderabad
        PINCODE_COORDINATES.put("500001", new Coordinates(17.3850, 78.4867));
        // Chennai
        PINCODE_COORDINATES.put("600001", new Coordinates(13.0827, 80.2707));
        // Kolkata
        PINCODE_COORDINATES.put("700001", new Coordinates(22.5726, 88.3639));
    }
    
    public MockRoutingService() {
        logger.info("Initialized Mock Routing Service (for testing/demo purposes)");
    }
    
    @Override
    public Route getRoute(Coordinates source, Coordinates destination) {
        logger.debug("Mock: Getting route from {} to {}", source, destination);
        
        // Generate a simple route with intermediate points
        List<Coordinates> pathPoints = generatePathPoints(source, destination);
        
        // Calculate approximate distance using haversine formula
        double distance = calculateDistance(source, destination);
        
        logger.info("Mock: Generated route with {} path points and distance {} km", 
                    pathPoints.size(), String.format("%.2f", distance));
        
        return new Route(source, destination, pathPoints, distance);
    }
    
    @Override
    public Coordinates getCoordinatesForPincode(String pincode) {
        logger.debug("Mock: Getting coordinates for pincode: {}", pincode);
        
        Coordinates coords = PINCODE_COORDINATES.get(pincode);
        
        if (coords == null) {
            // Generate mock coordinates based on pincode pattern
            // This is a fallback for pincodes not in our predefined map
            int pincodeNum = Integer.parseInt(pincode);
            double lat = 20.0 + (pincodeNum % 15);
            double lon = 72.0 + (pincodeNum % 15);
            coords = new Coordinates(lat, lon);
            logger.debug("Mock: Generated fallback coordinates for pincode {}: {}", pincode, coords);
        } else {
            logger.debug("Mock: Found predefined coordinates for pincode {}: {}", pincode, coords);
        }
        
        return coords;
    }
    
    /**
     * Generate intermediate path points between source and destination
     * Creates a realistic route that passes through known toll plaza locations
     */
    private List<Coordinates> generatePathPoints(Coordinates source, Coordinates destination) {
        List<Coordinates> points = new ArrayList<>();
        points.add(source);
        
        // Add known toll plaza coordinates for realistic routes
        addRealisticRoutePoints(points, source, destination);
        
        points.add(destination);
        return points;
    }
    
    /**
     * Add realistic intermediate points based on common routes
     */
    private void addRealisticRoutePoints(List<Coordinates> points, Coordinates source, Coordinates destination) {
        // Delhi to Gurgaon route - passes through Delhi-Gurgaon Toll Plaza
        if (isNear(source, 28.6139, 77.2090) && isNear(destination, 28.4595, 77.0266)) {
            points.add(new Coordinates(28.5500, 77.1200));
            points.add(new Coordinates(28.4595, 77.0266)); // Delhi-Gurgaon Toll Plaza
            return;
        }
        
        // Delhi to Bangalore route - passes through multiple toll plazas
        if (isNear(source, 28.6139, 77.2090) && isNear(destination, 12.9716, 77.5946)) {
            points.add(new Coordinates(28.4595, 77.0266)); // Delhi-Gurgaon Toll Plaza
            points.add(new Coordinates(20.0, 77.0));
            points.add(new Coordinates(15.0, 77.3));
            points.add(new Coordinates(12.8237, 77.3821)); // Bangalore-Mysore Toll Plaza
            return;
        }
        
        // Mumbai to Pune route - passes through Mumbai-Pune and Lonavala toll plazas
        if (isNear(source, 18.9388, 72.8354) && isNear(destination, 18.5204, 73.8567)) {
            points.add(new Coordinates(18.9894, 73.1178)); // Mumbai-Pune Toll Plaza
            points.add(new Coordinates(18.7537, 73.4086)); // Lonavala Toll Plaza
            return;
        }
        
        // Default: generate linear interpolation with slight variations
        int numPoints = 7;
        for (int i = 1; i < numPoints; i++) {
            double ratio = (double) i / numPoints;
            double lat = source.getLatitude() + (destination.getLatitude() - source.getLatitude()) * ratio;
            double lon = source.getLongitude() + (destination.getLongitude() - source.getLongitude()) * ratio;
            points.add(new Coordinates(lat, lon));
        }
    }
    
    /**
     * Check if coordinates are near a given location (within 0.1 degrees)
     */
    private boolean isNear(Coordinates coords, double lat, double lon) {
        return Math.abs(coords.getLatitude() - lat) < 0.1 && 
               Math.abs(coords.getLongitude() - lon) < 0.1;
    }
    
    /**
     * Calculate distance using haversine formula
     */
    private double calculateDistance(Coordinates c1, Coordinates c2) {
        final double EARTH_RADIUS_KM = 6371.0;
        
        double lat1Rad = Math.toRadians(c1.getLatitude());
        double lat2Rad = Math.toRadians(c2.getLatitude());
        double deltaLat = Math.toRadians(c2.getLatitude() - c1.getLatitude());
        double deltaLon = Math.toRadians(c2.getLongitude() - c1.getLongitude());
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
}
