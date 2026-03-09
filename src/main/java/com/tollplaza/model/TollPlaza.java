package com.tollplaza.model;

import java.util.Objects;

public class TollPlaza {
    private String name;
    private double latitude;
    private double longitude;

    public TollPlaza() {
    }

    public TollPlaza(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TollPlaza tollPlaza = (TollPlaza) o;
        return Double.compare(tollPlaza.latitude, latitude) == 0 &&
               Double.compare(tollPlaza.longitude, longitude) == 0 &&
               Objects.equals(name, tollPlaza.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, latitude, longitude);
    }

    @Override
    public String toString() {
        return "TollPlaza{" +
                "name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
