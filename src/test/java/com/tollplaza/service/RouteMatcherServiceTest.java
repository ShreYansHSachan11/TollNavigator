package com.tollplaza.service;

import com.tollplaza.model.Coordinates;
import com.tollplaza.model.Route;
import com.tollplaza.model.TollPlaza;
import com.tollplaza.model.TollPlazaMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteMatcherServiceTest {
    
    private RouteMatcherService routeMatcherService;
    
    @BeforeEach
    void setUp() {
        routeMatcherService = new RouteMatcherService();
        // Set proximity threshold to 2.0 km
        ReflectionTestUtils.setField(routeMatcherService, "proximityThresholdKm", 2.0);
    }
    
    @Test
    void testFindTollPlazasOnRoute_WithMatchingTollPlazas() {
        // Create a simple route from Delhi to Gurgaon
        Coordinates source = new Coordinates(28.6139, 77.2090); // Delhi
        Coordinates destination = new Coordinates(28.4595, 77.0266); // Gurgaon
        
        List<Coordinates> pathPoints = Arrays.asList(
            source,
            new Coordinates(28.5500, 77.1000),
            destination
        );
        
        Route route = new Route(source, destination, pathPoints, 30.0);
        
        // Create toll plazas - one on route, one off route
        List<TollPlaza> tollPlazas = Arrays.asList(
            new TollPlaza("Delhi-Gurgaon Toll", 28.5500, 77.1000), // On route
            new TollPlaza("Far Away Toll", 29.0000, 78.0000) // Off route
        );
        
        List<TollPlazaMatch> matches = routeMatcherService.findTollPlazasOnRoute(route, tollPlazas);
        
        // Should match only the toll plaza on the route
        assertEquals(1, matches.size());
        assertEquals("Delhi-Gurgaon Toll", matches.get(0).getTollPlaza().getName());
    }
    
    @Test
    void testFindTollPlazasOnRoute_NoMatchingTollPlazas() {
        Coordinates source = new Coordinates(28.6139, 77.2090);
        Coordinates destination = new Coordinates(28.4595, 77.0266);
        
        List<Coordinates> pathPoints = Arrays.asList(source, destination);
        Route route = new Route(source, destination, pathPoints, 30.0);
        
        // Create toll plazas far from the route
        List<TollPlaza> tollPlazas = Arrays.asList(
            new TollPlaza("Far Toll 1", 30.0000, 80.0000),
            new TollPlaza("Far Toll 2", 25.0000, 75.0000)
        );
        
        List<TollPlazaMatch> matches = routeMatcherService.findTollPlazasOnRoute(route, tollPlazas);
        
        assertEquals(0, matches.size());
    }
    
    @Test
    void testFindTollPlazasOnRoute_SortedByDistanceFromSource() {
        Coordinates source = new Coordinates(28.6139, 77.2090);
        Coordinates destination = new Coordinates(28.4595, 77.0266);
        
        List<Coordinates> pathPoints = Arrays.asList(
            source,
            new Coordinates(28.5800, 77.1500),
            new Coordinates(28.5200, 77.0800),
            destination
        );
        
        Route route = new Route(source, destination, pathPoints, 30.0);
        
        // Create toll plazas at different distances from source
        List<TollPlaza> tollPlazas = Arrays.asList(
            new TollPlaza("Toll 2", 28.5200, 77.0800), // Farther from source
            new TollPlaza("Toll 1", 28.5800, 77.1500)  // Closer to source
        );
        
        List<TollPlazaMatch> matches = routeMatcherService.findTollPlazasOnRoute(route, tollPlazas);
        
        assertEquals(2, matches.size());
        // Should be sorted by distance from source
        assertTrue(matches.get(0).getDistanceFromSource() < matches.get(1).getDistanceFromSource());
    }
    
    @Test
    void testFindTollPlazasOnRoute_NullRoute() {
        List<TollPlaza> tollPlazas = Arrays.asList(
            new TollPlaza("Test Toll", 28.5500, 77.1000)
        );
        
        List<TollPlazaMatch> matches = routeMatcherService.findTollPlazasOnRoute(null, tollPlazas);
        
        assertEquals(0, matches.size());
    }
    
    @Test
    void testFindTollPlazasOnRoute_NullTollPlazaList() {
        Coordinates source = new Coordinates(28.6139, 77.2090);
        Coordinates destination = new Coordinates(28.4595, 77.0266);
        List<Coordinates> pathPoints = Arrays.asList(source, destination);
        Route route = new Route(source, destination, pathPoints, 30.0);
        
        List<TollPlazaMatch> matches = routeMatcherService.findTollPlazasOnRoute(route, null);
        
        assertEquals(0, matches.size());
    }
    
    @Test
    void testFindTollPlazasOnRoute_EmptyPathPoints() {
        Coordinates source = new Coordinates(28.6139, 77.2090);
        Coordinates destination = new Coordinates(28.4595, 77.0266);
        Route route = new Route(source, destination, new ArrayList<>(), 30.0);
        
        List<TollPlaza> tollPlazas = Arrays.asList(
            new TollPlaza("Test Toll", 28.5500, 77.1000)
        );
        
        List<TollPlazaMatch> matches = routeMatcherService.findTollPlazasOnRoute(route, tollPlazas);
        
        assertEquals(0, matches.size());
    }
    
    @Test
    void testFindTollPlazasOnRoute_UniqueTollPlazas() {
        Coordinates source = new Coordinates(28.6139, 77.2090);
        Coordinates destination = new Coordinates(28.4595, 77.0266);
        
        List<Coordinates> pathPoints = Arrays.asList(
            source,
            new Coordinates(28.5500, 77.1000),
            destination
        );
        
        Route route = new Route(source, destination, pathPoints, 30.0);
        
        // Create duplicate toll plazas
        TollPlaza toll = new TollPlaza("Delhi-Gurgaon Toll", 28.5500, 77.1000);
        List<TollPlaza> tollPlazas = Arrays.asList(toll, toll);
        
        List<TollPlazaMatch> matches = routeMatcherService.findTollPlazasOnRoute(route, tollPlazas);
        
        // Should only match once (uniqueness)
        assertEquals(1, matches.size());
    }
}
