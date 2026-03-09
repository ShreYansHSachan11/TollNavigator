package com.tollplaza.service;

import com.tollplaza.model.Coordinates;
import com.tollplaza.model.Route;

public interface RoutingService {
    
    /**
     * Get route between source and destination coordinates
     * @param source Source coordinates
     * @param destination Destination coordinates
     * @return Route object with path points and distance
     */
    Route getRoute(Coordinates source, Coordinates destination);
    
    /**
     * Get coordinates for a given pincode using geocoding
     * @param pincode 6-digit Indian pincode
     * @return Coordinates for the pincode
     */
    Coordinates getCoordinatesForPincode(String pincode);
}
