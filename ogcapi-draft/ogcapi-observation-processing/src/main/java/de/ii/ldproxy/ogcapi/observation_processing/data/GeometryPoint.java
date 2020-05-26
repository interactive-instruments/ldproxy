package de.ii.ldproxy.ogcapi.observation_processing.data;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class GeometryPoint implements Geometry {
    final List<Float> coord;

    public GeometryPoint(List<Float> coord) {
        this.coord = coord;
    }

    public GeometryPoint(float lon, float lat) {
        this.coord = ImmutableList.of(lon, lat);
    }

    public GeometryPoint(float lon, float lat, float alt) {
        this.coord = ImmutableList.of(lon, lat, alt);
    }

    public float getLon() {
        return coord.size()>=1 ? coord.get(0) : Float.NaN;
    }

    public float getLat() {
        return coord.size()>=2 ? coord.get(1) : Float.NaN;
    }

    public float getAlt() {
        return coord.size()>=3 ? coord.get(2) : Float.NaN;
    }

    public List<Float> asList() { return coord; }
}
