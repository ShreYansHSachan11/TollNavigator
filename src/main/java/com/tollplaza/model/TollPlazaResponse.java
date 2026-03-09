package com.tollplaza.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Response containing route information and toll plazas found along the route")
public class TollPlazaResponse {
    
    @Schema(description = "Route information including source, destination, and total distance")
    private RouteInfo route;
    
    @Schema(description = "List of toll plazas found along the route, ordered by distance from source")
    private List<TollPlazaInfo> tollPlazas;

    public TollPlazaResponse() {
        this.tollPlazas = new ArrayList<>();
    }

    public TollPlazaResponse(RouteInfo route, List<TollPlazaInfo> tollPlazas) {
        this.route = route;
        this.tollPlazas = tollPlazas != null ? tollPlazas : new ArrayList<>();
    }

    public RouteInfo getRoute() {
        return route;
    }

    public void setRoute(RouteInfo route) {
        this.route = route;
    }

    public List<TollPlazaInfo> getTollPlazas() {
        return tollPlazas;
    }

    public void setTollPlazas(List<TollPlazaInfo> tollPlazas) {
        this.tollPlazas = tollPlazas;
    }

    @Schema(description = "Route information")
    public static class RouteInfo {
        
        @Schema(description = "Source pincode", example = "110001")
        private String sourcePincode;
        
        @Schema(description = "Destination pincode", example = "560001")
        private String destinationPincode;
        
        @Schema(description = "Total route distance in kilometers", example = "2100.5")
        private double distanceInKm;

        public RouteInfo() {
        }

        public RouteInfo(String sourcePincode, String destinationPincode, double distanceInKm) {
            this.sourcePincode = sourcePincode;
            this.destinationPincode = destinationPincode;
            this.distanceInKm = distanceInKm;
        }

        public String getSourcePincode() {
            return sourcePincode;
        }

        public void setSourcePincode(String sourcePincode) {
            this.sourcePincode = sourcePincode;
        }

        public String getDestinationPincode() {
            return destinationPincode;
        }

        public void setDestinationPincode(String destinationPincode) {
            this.destinationPincode = destinationPincode;
        }

        public double getDistanceInKm() {
            return distanceInKm;
        }

        public void setDistanceInKm(double distanceInKm) {
            this.distanceInKm = distanceInKm;
        }
    }

    @Schema(description = "Toll plaza information")
    public static class TollPlazaInfo {
        
        @Schema(description = "Name of the toll plaza", example = "Delhi-Gurgaon Toll Plaza")
        private String name;
        
        @Schema(description = "Latitude coordinate of toll plaza", example = "28.4595")
        private double latitude;
        
        @Schema(description = "Longitude coordinate of toll plaza", example = "77.0266")
        private double longitude;
        
        @Schema(description = "Distance from source pincode in kilometers", example = "15.3")
        private double distanceFromSource;

        public TollPlazaInfo() {
        }

        public TollPlazaInfo(String name, double latitude, double longitude, double distanceFromSource) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.distanceFromSource = distanceFromSource;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public double getDistanceFromSource() {
            return distanceFromSource;
        }

        public void setDistanceFromSource(double distanceFromSource) {
            this.distanceFromSource = distanceFromSource;
        }
    }
}
