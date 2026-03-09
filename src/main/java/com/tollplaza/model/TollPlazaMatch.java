package com.tollplaza.model;

import java.util.Objects;

public class TollPlazaMatch {
    private TollPlaza tollPlaza;
    private double distanceFromSource;

    public TollPlazaMatch() {
    }

    public TollPlazaMatch(TollPlaza tollPlaza, double distanceFromSource) {
        this.tollPlaza = tollPlaza;
        this.distanceFromSource = distanceFromSource;
    }

    public TollPlaza getTollPlaza() {
        return tollPlaza;
    }

    public void setTollPlaza(TollPlaza tollPlaza) {
        this.tollPlaza = tollPlaza;
    }

    public double getDistanceFromSource() {
        return distanceFromSource;
    }

    public void setDistanceFromSource(double distanceFromSource) {
        this.distanceFromSource = distanceFromSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TollPlazaMatch that = (TollPlazaMatch) o;
        return Double.compare(that.distanceFromSource, distanceFromSource) == 0 &&
               Objects.equals(tollPlaza, that.tollPlaza);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tollPlaza, distanceFromSource);
    }

    @Override
    public String toString() {
        return "TollPlazaMatch{" +
                "tollPlaza=" + tollPlaza +
                ", distanceFromSource=" + distanceFromSource +
                '}';
    }
}
