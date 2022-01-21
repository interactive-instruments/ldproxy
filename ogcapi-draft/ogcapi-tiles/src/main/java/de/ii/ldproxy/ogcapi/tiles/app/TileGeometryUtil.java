/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.locationtech.jts.simplify.VWSimplifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class TileGeometryUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(TileGeometryUtil.class);

    public static Geometry getTileGeometry(Geometry geom, AffineTransformation affineTransformation,
        Geometry clipGeometry, GeometryPrecisionReducer reducer, PrecisionModel precisionModel,
        double minimumSizeInPixel, double maxRelativeAreaChangeInPolygonRepair,
        double maxAbsoluteAreaChangeInPolygonRepair) {

        // The following changes are applied:
        // 1. The coordinates are converted to the tile coordinate system (0/0 is top left, 256/256 is bottom right)
        // 2. Small rings or line strings are dropped (small in the context of the tile, one pixel or less). The idea
        //    is to simply drop them as early as possible and before the next processing steps which may depend on
        //    having valid geometries and removing everything that will eventually be removed anyway helps.
        // 3. Remove unnecessary vertices and snap coordinates to the grid.
        // 4. If the resulting geometry is invalid polygonal geometry, try to make it valid.
        // 5. Hopefully we have a valid geometry now, so try to clip it to the tile.
        //
        // After each step, check, if we still have a geometry or the resulting tile geometry was too small for
        // the tile. In that case the feature is ignored.

        // 1 convert to the tile coordinate system
        geom.apply(affineTransformation);

        // 2 remove small rings or line strings (small in the context of the tile)
        geom = removeSmallPieces(geom, minimumSizeInPixel);
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        // 3 simplify the geometry
        geom = TopologyPreservingSimplifier.simplify(geom, 1.0/precisionModel.getScale());
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        // 4 reduce the geometry to the tile grid
        geom = reduce(geom, reducer, precisionModel, maxRelativeAreaChangeInPolygonRepair, maxAbsoluteAreaChangeInPolygonRepair);
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        // 5 if the resulting geometry is invalid, try to make it valid
        if (!geom.isValid()) {
            geom = repairPolygon(geom, maxRelativeAreaChangeInPolygonRepair, maxAbsoluteAreaChangeInPolygonRepair, 1.0/precisionModel.getScale());
            if (Objects.isNull(geom) || geom.isEmpty())
                return null;
        }

        // 6 limit the coordinates to the tile with a buffer
        geom = clipGeometry(geom, clipGeometry);
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        // reduce the geometry to the tile grid
        geom = reduce(geom, reducer, precisionModel, maxRelativeAreaChangeInPolygonRepair, maxAbsoluteAreaChangeInPolygonRepair);
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        // finally again remove any small rings or line strings created in the processing
        geom = removeSmallPieces(geom, minimumSizeInPixel);
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        if (!geom.isValid()) {
            // try once more
            LOGGER.debug("Polygonal geometry invalid after initial processing. Final attempt to repair.");

            geom = repairPolygon(geom, maxRelativeAreaChangeInPolygonRepair, maxAbsoluteAreaChangeInPolygonRepair, 1.0/precisionModel.getScale());
            if (Objects.isNull(geom) || geom.isEmpty())
                return null;

            // limit the coordinates to the tile with a buffer
            geom = clipGeometry(geom, clipGeometry);
            if (Objects.isNull(geom) || geom.isEmpty())
                return null;

            // reduce the geometry to the tile grid
            geom = reduce(geom, reducer, precisionModel, maxRelativeAreaChangeInPolygonRepair, maxAbsoluteAreaChangeInPolygonRepair);
            if (Objects.isNull(geom) || geom.isEmpty())
                return null;

            // finally again remove any small rings or line strings created in the processing
            geom = removeSmallPieces(geom, minimumSizeInPixel);
            if (Objects.isNull(geom) || geom.isEmpty())
                return null;
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

    private static Geometry clipGeometry(Geometry geometry, Geometry clipGeometry) {
        try {
            Geometry original = geometry;
            geometry = clipGeometry.intersection(original);

            // sometimes an intersection is returned as an empty geometry.
            // going via wkt fixes the problem.
            if (geometry.isEmpty() && original.intersects(clipGeometry)) {
                Geometry originalViaWkt = new WKTReader().read(original.toText());
                geometry = clipGeometry.intersection(originalViaWkt);
            }

        } catch (TopologyException | ParseException e) {
            // could not intersect or encode/decode WKT. original geometry will be used instead.
        }
        return geometry;
    }

    private static Polygon removeSmallPieces(Polygon geom, double minimumSizeInPixel) {
        if (geom.getArea() < minimumSizeInPixel * minimumSizeInPixel)
            // skip this feature, too small
            return null;
        List<LinearRing> holes= new ArrayList<>();
        boolean skipped = false;
        for (int i=0; i < geom.getNumInteriorRing(); i++) {
            LinearRing hole = geom.getInteriorRingN(i);
            if (geom.getFactory().createPolygon(hole).getArea() >= minimumSizeInPixel * minimumSizeInPixel) {
                holes.add(hole);
            } else
                skipped = true;
        }

        return skipped ? geom.getFactory().createPolygon(geom.getExteriorRing(), holes.toArray(LinearRing[]::new)) : geom;
    }

    private static Geometry removeSmallPieces(Geometry geom, double minimumSizeInPixel) {
        if (geom instanceof Polygon) {
            return removeSmallPieces((Polygon) geom, minimumSizeInPixel);
        } else if (geom instanceof MultiPolygon) {
            List<Polygon> patches = new ArrayList<>();
            boolean changed = false;
            for (int i=0; i < geom.getNumGeometries(); i++) {
                Polygon patch = (Polygon) geom.getGeometryN(i);
                Polygon newPolygon = removeSmallPieces(patch, minimumSizeInPixel);
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
            if (geom.getLength() < minimumSizeInPixel)
                // skip this feature, too small
                return null;
        } else if (geom instanceof MultiLineString) {
            List<LineString> segments = new ArrayList<>();
            boolean changed = false;
            for (int i=0; i < geom.getNumGeometries(); i++) {
                LineString segment = (LineString) geom.getGeometryN(i);
                if (segment.getLength() >= minimumSizeInPixel) {
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

    private static Geometry repairPolygon(Geometry geom, double maxRelativeAreaChangeInPolygonRepair, double maxAbsoluteAreaChangeInPolygonRepair, double distance) {
        if (geom instanceof Polygon || geom instanceof MultiPolygon) {
            // TODO update once JTS has a proper makeValid() capability

            // the standard fix for invalid, self-intersecting polygons is to use a zero-distance buffer;
            // however, this does not work in all cases and sometimes creates invalid or unwanted results,
            // for example, with "bow tie" geometries
            Geometry bufferGeom = geom.buffer(0.0);
            double areaChange = Math.abs(bufferGeom.getArea() - geom.getArea()) / geom.getArea();
            if ((areaChange <= maxRelativeAreaChangeInPolygonRepair || Math.abs(bufferGeom.getArea() - geom.getArea()) <= maxAbsoluteAreaChangeInPolygonRepair) && bufferGeom.isValid())
                return bufferGeom;
            LOGGER.trace("Buffer repair of polygonal geometry failed, valid={}, initial area {}, new area {}.", bufferGeom.isValid(), geom.getArea(), bufferGeom.getArea());

            // try to rebuild the polygon geometry
            try {
                Geometry newGeom = rebuildPolygon(geom);
                if (newGeom.isValid())
                    return newGeom;
                LOGGER.trace("Polygonal geometry rebuild failed, valid={}, initial area {}, new area {}.", newGeom.isValid(), geom.getArea(), newGeom.getArea());
            } catch (Exception e) {
                LOGGER.trace("Polygonal geometry rebuild failed due to a JTS exception.");
            }

            // try a union
            Geometry unionGeom = geom.union();
            areaChange = Math.abs(unionGeom.getArea() - geom.getArea()) / geom.getArea();
            if ((areaChange <= maxRelativeAreaChangeInPolygonRepair || Math.abs(unionGeom.getArea() - geom.getArea()) <= maxAbsoluteAreaChangeInPolygonRepair) && unionGeom.isValid())
                return unionGeom;
            LOGGER.trace("Union repair of polygonal geometry failed, valid={}, initial area {}, new area {}.", unionGeom.isValid(), geom.getArea(), unionGeom.getArea());

            // try a convex hull
            Geometry hullGeom = geom.convexHull();
            areaChange = Math.abs(hullGeom.getArea() - geom.getArea()) / geom.getArea();
            if ((areaChange <= maxRelativeAreaChangeInPolygonRepair || Math.abs(hullGeom.getArea() - geom.getArea()) <= maxAbsoluteAreaChangeInPolygonRepair) && hullGeom.isValid())
                return hullGeom;
            LOGGER.trace("Convex hull repair of polygonal geometry failed, valid={}, initial area {}, new area {}.", hullGeom.isValid(), geom.getArea(), hullGeom.getArea());

            // try the Visvalingam-Whyatt simplifier
            try {
                Geometry vwGeom = VWSimplifier.simplify(geom, distance);
                if (vwGeom.isValid())
                    return vwGeom;
                LOGGER.trace("Visvalingam-Whyatt simplification failed, valid={}, initial area {}, new area {}.", vwGeom.isValid(), geom.getArea(), vwGeom.getArea());
            } catch (Exception e) {
                LOGGER.trace("Visvalingam-Whyatt simplification failed due to a JTS exception.");
            }
        }

        return geom;
    }

    private static void addLinearRing(LinearRing geom, Polygonizer polygonizer) {
        LineString lineString = geom.getFactory().createLineString(geom.getCoordinateSequence());
        if (lineString.isValid() && lineString.isSimple())
            polygonizer.add(lineString);
        else
            polygonizer.add(lineString.union(lineString.getFactory().createPoint(lineString.getCoordinateN(0))));
    }

    private static void addPolygon(Polygon geom, Polygonizer polygonizer) {
        geom.normalize();
        if (geom.isValid())
            polygonizer.add(geom);
        else {
            addLinearRing(geom.getExteriorRing(), polygonizer);
            for (int i = 0; i < geom.getNumInteriorRing(); i++) {
                addLinearRing(geom.getInteriorRingN(i), polygonizer);
            }
        }
    }

    private static Geometry rebuildPolygon(Geometry geom) {
        Polygonizer polygonizer = new Polygonizer(true);
        if (geom instanceof Polygon) {
            addPolygon((Polygon)geom, polygonizer);
        } else {
            for (int i = 0; i < geom.getNumGeometries(); i++) {
                addPolygon((Polygon) geom.getGeometryN(i), polygonizer);
            }
        }
        Collection<Polygon> polygons = polygonizer.getPolygons();
        switch(polygons.size()){
            case 0:
                return geom.getFactory().createMultiPolygon();
            case 1:
                return polygons.iterator().next();
            default:
                try {
                    Iterator<Polygon> iter = polygons.iterator();
                    Geometry ret = iter.next();
                    while(iter.hasNext()){
                        ret = ret.symDifference(iter.next());
                    }
                    return ret;
                } catch (Exception e) {
                    return geom.getFactory().createMultiPolygon(polygons.toArray(Polygon[]::new));
                }
        }
    }

    private static Geometry reduce(Geometry geom, GeometryPrecisionReducer reducer, PrecisionModel precisionModel, double maxRelativeAreaChangeInPolygonRepair, double maxAbsoluteAreaChangeInPolygonRepair) {
        try {
            Geometry newGeom = reducer.reduce(geom);
            if (geom instanceof Polygon || geom instanceof MultiPolygon) {
                double areaChange = Math.abs(newGeom.getArea() - geom.getArea()) / geom.getArea();
                if (areaChange > maxRelativeAreaChangeInPolygonRepair &&
                        Math.abs(newGeom.getArea() - geom.getArea()) > maxAbsoluteAreaChangeInPolygonRepair) {
                    LOGGER.debug("Reduce failed, valid={}, initial area {}, new area {}.", newGeom.isValid(), geom.getArea(), newGeom.getArea());
                    newGeom = GeometryPrecisionReducer.reducePointwise(geom, precisionModel);
                }
            }
            return newGeom;
        } catch (Exception e) {
            return GeometryPrecisionReducer.reducePointwise(geom, precisionModel);
        }
    }
}


