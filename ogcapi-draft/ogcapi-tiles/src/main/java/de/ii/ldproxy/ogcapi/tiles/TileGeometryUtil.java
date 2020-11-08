package de.ii.ldproxy.ogcapi.tiles;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TileGeometryUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(TileGeometryUtil.class);

    static Geometry clipGeometry(Geometry geometry, Geometry clipGeometry) {
        try {
            Geometry original = geometry;
            geometry = clipGeometry.intersection(original);

            // some times a intersection is returned as an empty geometry.
            // going via wkt fixes the problem.
            if (geometry.isEmpty() && original.intersects(clipGeometry)) {
                Geometry originalViaWkt = new WKTReader().read(original.toText());
                geometry = clipGeometry.intersection(originalViaWkt);
            }

            return geometry;
        } catch (TopologyException e) {
            // could not intersect. original geometry will be used instead.
            return geometry;
        } catch (ParseException e1) {
            // could not encode/decode WKT. original geometry will be used
            // instead.
            return geometry;
        }
    }

    private static Polygon removeSmallPieces(Polygon geom) {
        if (geom.getArea() <= 1.0)
            // skip this feature, too small
            return null;
        List<LinearRing> holes= new ArrayList<>();
        boolean skipped = false;
        for (int i=0; i < geom.getNumInteriorRing(); i++) {
            LinearRing hole = geom.getInteriorRingN(i);
            if (geom.getFactory().createPolygon(hole).getArea() > 1.0) {
                holes.add(hole);
            } else
                skipped = true;
        }

        return skipped ? geom.getFactory().createPolygon(geom.getExteriorRing(), holes.toArray(LinearRing[]::new)) : geom;
    }

    static Geometry removeSmallPieces(Geometry geom) {
        if (geom instanceof Polygon) {
            return removeSmallPieces((Polygon) geom);
        } else if (geom instanceof MultiPolygon) {
            List<Polygon> patches = new ArrayList<>();
            boolean changed = false;
            for (int i=0; i < geom.getNumGeometries(); i++) {
                Polygon patch = (Polygon) geom.getGeometryN(i);
                Polygon newPolygon = removeSmallPieces(patch);
                if (Objects.nonNull(newPolygon)) {
                    patches.add(newPolygon);
                    if (!Objects.equals(patch, newPolygon))
                        changed = true;
                } else {
                    changed = true;
                }
            }
            return changed ? geom.getFactory().createMultiPolygon(patches.toArray(Polygon[]::new)) : geom;
        } else if (geom instanceof LineString) {
            if (geom.getLength() <= 1.0)
                // skip this feature, too small
                return null;
        } else if (geom instanceof MultiLineString) {
            List<LineString> segments = new ArrayList<>();
            boolean changed = false;
            for (int i=0; i < geom.getNumGeometries(); i++) {
                LineString segment = (LineString) geom.getGeometryN(i);
                if (segment.getLength() > 1.0) {
                    segments.add(segment);
                } else {
                    // skip this feature, too small
                    changed = true;
                }
            }
            return changed ? geom.getFactory().createMultiLineString(segments.toArray(LineString[]::new)) : geom;
        }

        return geom;
    }

    static List<Polygon> splitMultiPolygon(MultiPolygon geom) {
        List<Polygon> patches = new ArrayList<>();
        for (int i=0; i < geom.getNumGeometries(); i++) {
            patches.add((Polygon) geom.getGeometryN(i));
        }
        return patches;
    }

    static List<LineString> splitMultiLineString(MultiLineString geom) {
        List<LineString> segments = new ArrayList<>();
        for (int i=0; i < geom.getNumGeometries(); i++) {
            segments.add((LineString) geom.getGeometryN(i));
        }
        return segments;
    }

    static Geometry processPolygons(Geometry geom, GeometryPrecisionReducer reducer, double distanceTolerance) {
        boolean reduceAgain = false;
        if (geom instanceof Polygon || geom instanceof MultiPolygon) {
            // the standard fix for invalid, self-intersecting polygons is to use a zero-distance buffer;
            // however, this does not work in all cases and sometimes creates invalid or unwanted results;
            // if the deviation is too big or invalid, we use the convex hull instead
            Geometry bufferGeom = geom.buffer(0.0);
            double areaChange = Math.abs(bufferGeom.getArea() - geom.getArea()) / geom.getArea();
            if (areaChange > 1.1 || areaChange < 0.9 || !bufferGeom.isValid()) {
                geom = geom.convexHull();
            } else {
                geom = bufferGeom;
            }

            // simplify the geometry, if the geometry hasn't been simplified before
            if (distanceTolerance != Double.NaN)
                geom = TopologyPreservingSimplifier.simplify(geom, distanceTolerance);

            reduceAgain = true;
        }

        // remove small rings or line strings (small in the context of the tile) that may
        // have been created in some cases by changing to the tile grid
        geom = TileGeometryUtil.removeSmallPieces(geom);
        if (geom==null || geom.isEmpty()) {
            return null;
        }

        // make sure the geometry is using the tile grid, if we processed a polygon geometry
        if (reduceAgain)
            geom = reducer.reduce(geom);

        return geom;
    }

    static Geometry processLineStrings(List<LineString> geoms, GeometryPrecisionReducer reducer, double distanceTolerance) {
        LineMerger merger = new LineMerger();
        merger.add(geoms);
        Geometry geom = geoms.get(0).getFactory().createMultiLineString((LineString[]) merger.getMergedLineStrings().toArray());

        // remove small line strings (small in the context of the tile)
        geom = TileGeometryUtil.removeSmallPieces(geom);
        if (geom==null) {
            return null;
        }

        // simplify the geometry
        geom = TopologyPreservingSimplifier.simplify(geom, distanceTolerance);

        // make sure the geometry is using the tile grid
        return reducer.reduce(geom);
    }


    static Geometry validate(Geometry geom) {
        if (geom instanceof Polygon || geom instanceof MultiPolygon) {
            // The standard recommendation in JTS to fix invalid (multi-)polygons is to try a buffer with distance 0.0.
            return geom.buffer(0.0);
        }

        return geom;
    }
}


