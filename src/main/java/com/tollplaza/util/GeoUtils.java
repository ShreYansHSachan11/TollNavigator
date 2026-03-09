package com.tollplaza.util;

import com.tollplaza.model.Coordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for geographic calculations.
 */
public class GeoUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(GeoUtils.class);
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    private GeoUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Calculates the distance between two coordinates using the Haversine formula.
     * 
     * @param coord1 First coordinate
     * @param coord2 Second coordinate
     * @return Distance in kilometers
     */
    public static double calculateDistance(Coordinates coord1, Coordinates coord2) {
        if (coord1 == null || coord2 == null) {
            logger.error("Attempted to calculate distance with null coordinates");
            throw new IllegalArgumentException("Coordinates cannot be null");
        }
        
        logger.debug("Calculating distance between ({}, {}) and ({}, {})", 
                    coord1.getLatitude(), coord1.getLongitude(),
                    coord2.getLatitude(), coord2.getLongitude());
        
        double lat1Rad = Math.toRadians(coord1.getLatitude());
        double lat2Rad = Math.toRadians(coord2.getLatitude());
        double deltaLatRad = Math.toRadians(coord2.getLatitude() - coord1.getLatitude());
        double deltaLonRad = Math.toRadians(coord2.getLongitude() - coord1.getLongitude());
        
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        double distance = EARTH_RADIUS_KM * c;
        logger.debug("Calculated distance: {} km", distance);
        
        return distance;
    }
    
    /**
     * Calculates the perpendicular distance from a point to a line segment.
     * 
     * @param point The point to measure from
     * @param lineStart Start of the line segment
     * @param lineEnd End of the line segment
     * @return Distance in kilometers
     */
    public static double distanceFromPointToLine(Coordinates point, Coordinates lineStart, Coordinates lineEnd) {
        if (point == null || lineStart == null || lineEnd == null) {
            logger.error("Attempted to calculate point-to-line distance with null coordinates");
            throw new IllegalArgumentException("Coordinates cannot be null");
        }
        
        logger.debug("Calculating distance from point ({}, {}) to line segment", 
                    point.getLatitude(), point.getLongitude());
        
        // If line start and end are the same, return distance to that point
        if (lineStart.equals(lineEnd)) {
            logger.debug("Line segment has same start and end point, calculating direct distance");
            return calculateDistance(point, lineStart);
        }
        
        // Convert to radians for calculations
        double lat1 = Math.toRadians(lineStart.getLatitude());
        double lon1 = Math.toRadians(lineStart.getLongitude());
        double lat2 = Math.toRadians(lineEnd.getLatitude());
        double lon2 = Math.toRadians(lineEnd.getLongitude());
        double lat3 = Math.toRadians(point.getLatitude());
        double lon3 = Math.toRadians(point.getLongitude());
        
        // Calculate cross-track distance
        double d13 = calculateDistance(lineStart, point) / EARTH_RADIUS_KM;
        double bearing13 = calculateBearing(lineStart, point);
        double bearing12 = calculateBearing(lineStart, lineEnd);
        
        double crossTrackDistance = Math.asin(Math.sin(d13) * Math.sin(bearing13 - bearing12)) * EARTH_RADIUS_KM;
        
        // Calculate along-track distance to find if point projects onto segment
        double alongTrackDistance = Math.acos(Math.cos(d13) / Math.cos(crossTrackDistance / EARTH_RADIUS_KM)) * EARTH_RADIUS_KM;
        
        double segmentLength = calculateDistance(lineStart, lineEnd);
        
        // Check if the projection falls within the line segment
        if (alongTrackDistance < 0) {
            // Point is before the start of the segment
            return calculateDistance(point, lineStart);
        } else if (alongTrackDistance > segmentLength) {
            // Point is after the end of the segment
            return calculateDistance(point, lineEnd);
        } else {
            // Point projects onto the segment
            return Math.abs(crossTrackDistance);
        }
    }
    
    /**
     * Calculates the bearing from one coordinate to another.
     * 
     * @param from Starting coordinate
     * @param to Ending coordinate
     * @return Bearing in radians
     */
    private static double calculateBearing(Coordinates from, Coordinates to) {
        double lat1 = Math.toRadians(from.getLatitude());
        double lat2 = Math.toRadians(to.getLatitude());
        double deltaLon = Math.toRadians(to.getLongitude() - from.getLongitude());
        
        double y = Math.sin(deltaLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                   Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);
        
        return Math.atan2(y, x);
    }
}
