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

    static Geometry validate(Geometry geom) {
        if (geom instanceof Polygon || geom instanceof MultiPolygon) {
            // The standard recommendation in JTS to fix invalid (multi-)polygons is to try a buffer with distance 0.0.
            return geom.buffer(0.0);
        }

        return geom;
    }
}
