/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.data;

import com.google.common.collect.ImmutableList;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

import java.util.List;
import java.util.Vector;

public class GeometryMultiPolygon implements Geometry {
    final List<List<List<List<Double>>>> coord;

    public GeometryMultiPolygon(List<List<List<List<Double>>>> coord) {
        this.coord = coord;
    }

    public GeometryMultiPolygon(org.locationtech.jts.geom.Geometry geometry) {
        List<List<List<List<Double>>>> mp = new Vector<>();
        if (geometry instanceof MultiPolygon) {
            MultiPolygon multiPolygon = (MultiPolygon) geometry;
            for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                mp.add(getPolygon((Polygon) multiPolygon.getGeometryN(i)));
            }
        } else if (geometry instanceof Polygon) {
            mp.add(getPolygon((Polygon) geometry));
        } else {
            throw new IllegalArgumentException(String.format("Geometry must be a (multi-)polygon. Found: '%s'",geometry.getGeometryType()));
        }
        this.coord = mp;
    }

    private List<List<List<Double>>> getPolygon(Polygon polygon) {
        int size = polygon.getNumInteriorRing() + 1;
        List<List<List<Double>>> p = new Vector<>();
        p.add(getLineString(polygon.getExteriorRing()));
        for (int i = 0; i < size - 1; i++) {
            p.add(getLineString(polygon.getInteriorRingN(i)));
        }
        return p;
    }

    private List<List<Double>> getLineString(LineString lineString) {
        List<List<Double>> ls = new Vector<>();
        Coordinate[] coordinates = lineString.getCoordinates();
        for (int i = 0; i < coordinates.length; i++) {
            ls.add(getPoint(coordinates[i]));
        }
        return ls;
    }

    private List<Double> getPoint(Coordinate coordinate) {
        if(Double.isNaN(coordinate.getZ())) {
            return ImmutableList.of(coordinate.getX(), coordinate.getY());
        }

        return ImmutableList.of(coordinate.getX(), coordinate.getY(), coordinate.getZ());
    }

    public int size() { return coord.size(); }

    public List<List<List<List<Double>>>> asList() { return coord; }

    public double[] getBbox() {
        final double[] bbox = {180f,90f,-180f,-90f};
        coord.stream().forEachOrdered(polygon -> {
            polygon.stream().forEachOrdered(ring -> {
                ring.stream().forEachOrdered(pos -> {
                    double lon = pos.size()>=1 ? pos.get(0) : Float.NaN;
                    if (lon < bbox[0])
                        bbox[0] = lon;
                    if (lon > bbox[2])
                        bbox[2] = lon;
                    double lat = pos.size()>=2 ? pos.get(1) : Float.NaN;
                    if (lat < bbox[1])
                        bbox[1] = lat;
                    if (lat > bbox[3])
                        bbox[3] = lat;
                });
            });
        });
        return bbox;
    }
}
