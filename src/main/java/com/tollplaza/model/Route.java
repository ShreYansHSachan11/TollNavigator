package com.tollplaza.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Route {
    private Coordinates source;
    private Coordinates destination;
    private List<Coordinates> pathPoints;
    private double distanceInKm;

    public Route() {
        this.pathPoints = new ArrayList<>();
    }

    public Route(Coordinates source, Coordinates destination, List<Coordinates> pathPoints, double distanceInKm) {
        this.source = source;
        this.destination = destination;
        this.pathPoints = pathPoints != null ? pathPoints : new ArrayList<>();
        this.distanceInKm = distanceInKm;
    }

    public Coordinates getSource() {
        return source;
    }

    public void setSource(Coordinates source) {
        this.source = source;
    }

    public Coordinates getDestination() {
        return destination;
    }

    public void setDestination(Coordinates destination) {
        this.destination = destination;
    }

    public List<Coordinates> getPathPoints() {
        return pathPoints;
    }

    public void setPathPoints(List<Coordinates> pathPoints) {
        this.pathPoints = pathPoints;
    }

    public double getDistanceInKm() {
        return distanceInKm;
    }

    public void setDistanceInKm(double distanceInKm) {
        this.distanceInKm = distanceInKm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return Double.compare(route.distanceInKm, distanceInKm) == 0 &&
               Objects.equals(source, route.source) &&
               Objects.equals(destination, route.destination) &&
               Objects.equals(pathPoints, route.pathPoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, destination, pathPoints, distanceInKm);
    }

    @Override
    public String toString() {
        return "Route{" +
                "source=" + source +
                ", destination=" + destination +
                ", pathPoints=" + pathPoints.size() + " points" +
                ", distanceInKm=" + distanceInKm +
                '}';
    }
}
