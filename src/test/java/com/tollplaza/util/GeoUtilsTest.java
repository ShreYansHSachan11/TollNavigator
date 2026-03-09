package com.tollplaza.util;

import com.tollplaza.model.Coordinates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilsTest {

    @Test
    void testCalculateDistance_SameCoordinates() {
        Coordinates coord1 = new Coordinates(28.7041, 77.1025);
        Coordinates coord2 = new Coordinates(28.7041, 77.1025);
        
        double distance = GeoUtils.calculateDistance(coord1, coord2);
        
        assertEquals(0.0, distance, 0.001);
    }

    @Test
    void testCalculateDistance_KnownDistance() {
        // Delhi to Mumbai approximate coordinates
        Coordinates delhi = new Coordinates(28.7041, 77.1025);
        Coordinates mumbai = new Coordinates(19.0760, 72.8777);
        
        double distance = GeoUtils.calculateDistance(delhi, mumbai);
        
        // Expected distance is approximately 1150-1200 km
        assertTrue(distance > 1100 && distance < 1250, 
                   "Distance should be approximately 1150-1200 km, got: " + distance);
    }

    @Test
    void testCalculateDistance_NullCoordinates() {
        Coordinates coord = new Coordinates(28.7041, 77.1025);
        
        assertThrows(IllegalArgumentException.class, 
                     () -> GeoUtils.calculateDistance(null, coord));
        assertThrows(IllegalArgumentException.class, 
                     () -> GeoUtils.calculateDistance(coord, null));
    }

    @Test
    void testDistanceFromPointToLine_PointOnLine() {
        Coordinates lineStart = new Coordinates(0.0, 0.0);
        Coordinates lineEnd = new Coordinates(0.0, 1.0);
        Coordinates point = new Coordinates(0.0, 0.5);
        
        double distance = GeoUtils.distanceFromPointToLine(point, lineStart, lineEnd);
        
        // Point is on the line, distance should be very small
        assertTrue(distance < 1.0, "Distance should be very small, got: " + distance);
    }

    @Test
    void testDistanceFromPointToLine_SameStartEnd() {
        Coordinates linePoint = new Coordinates(28.7041, 77.1025);
        Coordinates point = new Coordinates(28.7141, 77.1125);
        
        double distance = GeoUtils.distanceFromPointToLine(point, linePoint, linePoint);
        double expectedDistance = GeoUtils.calculateDistance(point, linePoint);
        
        assertEquals(expectedDistance, distance, 0.001);
    }

    @Test
    void testDistanceFromPointToLine_NullCoordinates() {
        Coordinates coord = new Coordinates(28.7041, 77.1025);
        
        assertThrows(IllegalArgumentException.class, 
                     () -> GeoUtils.distanceFromPointToLine(null, coord, coord));
        assertThrows(IllegalArgumentException.class, 
                     () -> GeoUtils.distanceFromPointToLine(coord, null, coord));
        assertThrows(IllegalArgumentException.class, 
                     () -> GeoUtils.distanceFromPointToLine(coord, coord, null));
    }
}
