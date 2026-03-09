package com.tollplaza.service;

import com.tollplaza.exception.InvalidPincodeException;
import com.tollplaza.model.Coordinates;
import com.tollplaza.model.Route;
import com.tollplaza.model.TollPlaza;
import com.tollplaza.model.TollPlazaMatch;
import com.tollplaza.model.TollPlazaResponse;
import com.tollplaza.repository.TollPlazaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for finding toll plazas between two pincodes.
 */
@Service
public class TollPlazaService {
    
    private static final Logger logger = LoggerFactory.getLogger(TollPlazaService.class);
    
    private final RoutingService routingService;
    private final RouteMatcherService routeMatcherService;
    private final TollPlazaRepository tollPlazaRepository;
    
    public TollPlazaService(RoutingService routingService, 
                           RouteMatcherService routeMatcherService,
                           TollPlazaRepository tollPlazaRepository) {
        this.routingService = routingService;
        this.routeMatcherService = routeMatcherService;
        this.tollPlazaRepository = tollPlazaRepository;
    }
    
    /**
     * Finds toll plazas between source and destination pincodes.
     * Results are cached based on the pincode pair.
     * 
     * @param sourcePincode Source pincode (6 digits)
     * @param destinationPincode Destination pincode (6 digits)
     * @return TollPlazaResponse containing route info and matched toll plazas
     * @throws InvalidPincodeException if pincodes are the same
     */
    @Cacheable(value = "tollPlazaRoutes", key = "#sourcePincode + '-' + #destinationPincode")
    public TollPlazaResponse findTollPlazas(String sourcePincode, String destinationPincode) {
        logger.info("Finding toll plazas between {} and {}", sourcePincode, destinationPincode);
        
        // Validate pincodes are different
        if (sourcePincode.equals(destinationPincode)) {
            logger.warn("Source and destination pincodes are the same: {}", sourcePincode);
            throw new InvalidPincodeException("Source and destination pincodes cannot be the same");
        }
        
        // Get coordinates for source and destination pincodes
        logger.debug("Getting coordinates for pincodes");
        Coordinates sourceCoords = routingService.getCoordinatesForPincode(sourcePincode);
        Coordinates destCoords = routingService.getCoordinatesForPincode(destinationPincode);
        
        // Get route from routing service
        logger.debug("Getting route from routing service");
        Route route = routingService.getRoute(sourceCoords, destCoords);
        
        // Get all toll plazas from repository
        logger.debug("Loading toll plazas from repository");
        List<TollPlaza> allTollPlazas = tollPlazaRepository.findAll();
        
        // Match toll plazas to route
        logger.debug("Matching toll plazas to route");
        List<TollPlazaMatch> matches = routeMatcherService.findTollPlazasOnRoute(route, allTollPlazas);
        
        // Build response
        TollPlazaResponse response = buildResponse(sourcePincode, destinationPincode, route, matches);
        
        logger.info("Found {} toll plazas on route from {} to {}", 
                   matches.size(), sourcePincode, destinationPincode);
        
        return response;
    }
    
    /**
     * Builds the TollPlazaResponse from route and matches.
     * 
     * @param sourcePincode Source pincode
     * @param destinationPincode Destination pincode
     * @param route Route object
     * @param matches List of toll plaza matches
     * @return TollPlazaResponse
     */
    private TollPlazaResponse buildResponse(String sourcePincode, String destinationPincode, 
                                           Route route, List<TollPlazaMatch> matches) {
        // Create route info
        TollPlazaResponse.RouteInfo routeInfo = new TollPlazaResponse.RouteInfo(
            sourcePincode,
            destinationPincode,
            route.getDistanceInKm()
        );
        
        // Convert matches to toll plaza info
        List<TollPlazaResponse.TollPlazaInfo> tollPlazaInfos = matches.stream()
            .map(match -> new TollPlazaResponse.TollPlazaInfo(
                match.getTollPlaza().getName(),
                match.getTollPlaza().getLatitude(),
                match.getTollPlaza().getLongitude(),
                match.getDistanceFromSource()
            ))
            .collect(Collectors.toList());
        
        return new TollPlazaResponse(routeInfo, tollPlazaInfos);
    }
}
