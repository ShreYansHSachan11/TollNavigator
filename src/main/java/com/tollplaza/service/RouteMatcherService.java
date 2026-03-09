package com.tollplaza.service;

import com.tollplaza.model.Coordinates;
import com.tollplaza.model.Route;
import com.tollplaza.model.TollPlaza;
import com.tollplaza.model.TollPlazaMatch;
import com.tollplaza.util.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for matching toll plazas to routes.
 */
@Service
public class RouteMatcherService {
    
    private static final Logger logger = LoggerFactory.getLogger(RouteMatcherService.class);
    
    @Value("${tollplaza.route.proximity-threshold-km:2.0}")
    private double proximityThresholdKm;
    
    /**
     * Finds toll plazas that are on or near the given route.
     * 
     * @param route The route to match against
     * @param allTollPlazas All available toll plazas
     * @return List of toll plaza matches sorted by distance from source
     */
    public List<TollPlazaMatch> findTollPlazasOnRoute(Route route, List<TollPlaza> allTollPlazas) {
        if (route == null || allTollPlazas == null) {
            logger.warn("Route or toll plaza list is null");
            return new ArrayList<>();
        }
        
        if (route.getPathPoints() == null || route.getPathPoints().isEmpty()) {
            logger.warn("Route has no path points");
            return new ArrayList<>();
        }
        
        logger.debug("Matching {} toll plazas against route with {} path points", 
                     allTollPlazas.size(), route.getPathPoints().size());
        
        Set<TollPlaza> matchedTollPlazas = new HashSet<>();
        List<TollPlazaMatch> matches = new ArrayList<>();
        
        for (TollPlaza tollPlaza : allTollPlazas) {
            // Skip if already matched (ensure uniqueness)
            if (matchedTollPlazas.contains(tollPlaza)) {
                continue;
            }
            
            Coordinates tollPlazaCoords = new Coordinates(tollPlaza.getLatitude(), tollPlaza.getLongitude());
            
            // Check if toll plaza is within proximity threshold of any route segment
            if (isOnRoute(tollPlazaCoords, route)) {
                double distanceFromSource = calculateDistanceFromSource(route.getSource(), tollPlazaCoords);
                matches.add(new TollPlazaMatch(tollPlaza, distanceFromSource));
                matchedTollPlazas.add(tollPlaza);
                logger.debug("Matched toll plaza: {} at distance {} km from source", 
                           tollPlaza.getName(), distanceFromSource);
            }
        }
        
        // Sort by distance from source
        matches.sort(Comparator.comparingDouble(TollPlazaMatch::getDistanceFromSource));
        
        logger.info("Found {} toll plazas on route", matches.size());
        return matches;
    }
    
    /**
     * Checks if a toll plaza is within the proximity threshold of the route.
     * 
     * @param tollPlazaCoords Coordinates of the toll plaza
     * @param route The route to check against
     * @return true if the toll plaza is on the route, false otherwise
     */
    private boolean isOnRoute(Coordinates tollPlazaCoords, Route route) {
        List<Coordinates> pathPoints = route.getPathPoints();
        
        // Check distance to each segment of the route
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Coordinates segmentStart = pathPoints.get(i);
            Coordinates segmentEnd = pathPoints.get(i + 1);
            
            double distance = GeoUtils.distanceFromPointToLine(tollPlazaCoords, segmentStart, segmentEnd);
            
            if (distance <= proximityThresholdKm) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Calculates the straight-line distance from the source to the toll plaza.
     * 
     * @param source Source coordinates
     * @param tollPlazaCoords Toll plaza coordinates
     * @return Distance in kilometers
     */
    private double calculateDistanceFromSource(Coordinates source, Coordinates tollPlazaCoords) {
        return GeoUtils.calculateDistance(source, tollPlazaCoords);
    }
}
