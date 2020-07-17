package de.ii.ldproxy.ogcapi.observation_processing.data;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class GeometryPoint implements Geometry {
    final List<Double> coord;

    public GeometryPoint(List<Double> coord) {
        this.coord = coord;
    }

    public GeometryPoint(double lon, double lat) {
        this.coord = ImmutableList.of(lon, lat);
    }

    public GeometryPoint(double lon, double lat, double alt) {
        this.coord = ImmutableList.of(lon, lat, alt);
    }

    public Double getLon() {
        return coord.size()>=1 ? coord.get(0) : Double.NaN;
    }

    public Double getLat() {
        return coord.size()>=2 ? coord.get(1) : Double.NaN;
    }

    public Double getAlt() {
        return coord.size()>=3 ? coord.get(2) : Double.NaN;
    }

    public List<Double> asList() { return coord; }
}
