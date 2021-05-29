/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This is a local copy / derived work with some edits just for testing the JTS GeometryFixer code.
 * TODO This will be removed after the next JTS release and before merging this branch.
 *
 * The original work is:
 *
 * Copyright (c) 2021 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package de.ii.ldproxy.ogcapi.tiles.domain;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateArrays;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.overlayng.OverlayNG;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;

/**
 * Fixes a geometry to be a valid geometry, while preserving as much as
 * possible of the shape and location of the input.
 * Validity is determined according to {@link Geometry#isValid()}.
 * <p>
 * Input geometries are always processed, so even valid inputs may
 * have some minor alterations.  The output is always a new geometry object.
 * <h2>Semantic Rules</h2>
 * <ol>
 * <li>Vertices with non-finite X or Y ordinates are removed.</li>
 * <li>Repeated points are reduced to a single point</li>
 * <li>Empty atomic geometries are valid and are returned unchanged</li>
 * <li>Empty elements are removed from collections</li>
 * <li><code>Point</code>: keep valid coordinate, or EMPTY</li>
 * <li><code>LineString</code>: fix coordinate list</li>
 * <li><code>LinearRing</code>: fix coordinate list, return as valid ring or else <code>LineString</code></li>
 * <li><code>Polygon</code>: transform into a valid polygon,
 * preserving as much of the extent and vertices as possible</li>
 * <li><code>MultiPolygon</code>: fix each polygon,
 * then ensure result is non-overlapping (via union)</li>
 * <li><code>GeometryCollection</code>: fix each element</li>
 * <li>Collapsed lines and polygons are handled as follows,
 * depending on the <code>keepCollapsed</code> setting:
 * <ul>
 * <li><code>false</code>: (default) collapses are converted to empty geometries</li>
 * <li><code>true</code>: collapses are converted to a valid geometry of lower dimension</li>
 * </ul>
 * </li>
 * </ol>
 *
 * @author Martin Davis
 *
 * @see Geometry#isValid()
 */
public class GeometryFixer {

    /**
     * Fixes a geometry to be valid.
     *
     * @param geom the geometry to be fixed
     * @return the valid fixed geometry
     */
    public static Geometry fix(Geometry geom) {
        GeometryFixer fix = new GeometryFixer(geom);
        return fix.getResult();
    }

    private Geometry geom;
    private GeometryFactory factory;
    private boolean isKeepCollapsed = false;

    /**
     * Creates a new instance to fix a given geometry.
     *
     * @param geom the geometry to be fixed
     */
    public GeometryFixer(Geometry geom) {
        this.geom = geom;
        this.factory = geom.getFactory();
    }

    /**
     * Sets whether collapsed geometries are converted to empty,
     * (which will be removed from collections),
     * or to a valid geometry of lower dimension.
     * The default is to convert collapses to empty geometries.
     *
     * @param isKeepCollapsed whether collapses should be converted to a lower dimension geometry
     */
    public void setKeepCollapsed(boolean isKeepCollapsed) {
        this.isKeepCollapsed  = isKeepCollapsed;
    }

    /**
     * Gets the fixed geometry.
     *
     * @return the fixed geometry
     */
    public Geometry getResult() {
        /**
         *  Truly empty geometries are simply copied.
         *  Geometry collections with elements are evaluated on a per-element basis.
         */
        if (geom.getNumGeometries() == 0) {
            return geom.copy();
        }

        if (geom instanceof Point)              return fixPoint((Point) geom);
        //  LinearRing must come before LineString
        if (geom instanceof LinearRing)         return fixLinearRing((LinearRing) geom);
        if (geom instanceof LineString)         return fixLineString((LineString) geom);
        if (geom instanceof Polygon)            return fixPolygon((Polygon) geom);
        if (geom instanceof MultiPoint)         return fixMultiPoint((MultiPoint) geom);
        if (geom instanceof MultiLineString)    return fixMultiLineString((MultiLineString) geom);
        if (geom instanceof MultiPolygon)       return fixMultiPolygon((MultiPolygon) geom);
        if (geom instanceof GeometryCollection) return fixCollection((GeometryCollection) geom);
        throw new UnsupportedOperationException(geom.getClass().getName());
    }

    private Point fixPoint(Point geom) {
        Geometry pt = fixPointElement(geom);
        if (pt == null)
            return factory.createPoint();
        return (Point) pt;
    }

    private Point fixPointElement(Point geom) {
        if (geom.isEmpty() || ! isValidPoint(geom)) {
            return null;
        }
        return (Point) geom.copy();
    }

    private static boolean isValidPoint(Point pt) {
        Coordinate p = pt.getCoordinate();
        return isValid(p);
    }

    private Geometry fixMultiPoint(MultiPoint geom) {
        List<Point> pts = new ArrayList<Point>();
        for (int i = 0; i < geom.getNumGeometries(); i++) {
            Point pt = (Point) geom.getGeometryN(i);
            if (pt.isEmpty()) continue;
            Point fixPt = fixPointElement(pt);
            if (fixPt != null) {
                pts.add(fixPt);
            }
        }
        return factory.createMultiPoint(GeometryFactory.toPointArray(pts));
    }

    private Geometry fixLinearRing(LinearRing geom) {
        Geometry fix = fixLinearRingElement(geom);
        if (fix == null)
            return factory.createLinearRing();
        return fix;
    }

    private Geometry fixLinearRingElement(LinearRing geom) {
        if (geom.isEmpty()) return null;
        Coordinate[] pts = geom.getCoordinates();
        Coordinate[] ptsFix = fixCoordinates(pts);
        if (isKeepCollapsed) {
            if (ptsFix.length == 1) {
                return factory.createPoint(ptsFix[0]);
            }
            if (ptsFix.length > 1 && ptsFix.length <= 3) {
                return factory.createLineString(ptsFix);
            }
        }
        //--- too short to be a valid ring
        if (ptsFix.length <= 3) {
            return null;
        }

        LinearRing ring = factory.createLinearRing(ptsFix);
        //--- convert invalid ring to LineString
        if (! ring.isValid()) {
            return factory.createLineString(ptsFix);
        }
        return ring;
    }

    private Geometry fixLineString(LineString geom) {
        Geometry fix = fixLineStringElement(geom);
        if (fix == null)
            return factory.createLineString();
        return fix;
    }

    private Geometry fixLineStringElement(LineString geom) {
        if (geom.isEmpty()) return null;
        Coordinate[] pts = geom.getCoordinates();
        Coordinate[] ptsFix = fixCoordinates(pts);
        if (isKeepCollapsed && ptsFix.length == 1) {
            return factory.createPoint(ptsFix[0]);
        }
        if (ptsFix.length <= 1) {
            return null;
        }
        return factory.createLineString(ptsFix);
    }

    /**
     * Returns a clean copy of the input coordinate array.
     *
     * @param pts coordinates to clean
     * @return an array of clean coordinates
     */
    private static Coordinate[] fixCoordinates(Coordinate[] pts) {
        Coordinate[] ptsClean = removeRepeatedAndInvalidPoints(pts);
        return CoordinateArrays.copyDeep(ptsClean);
    }

    private Geometry fixMultiLineString(MultiLineString geom) {
        List<Geometry> fixed = new ArrayList<Geometry>();
        boolean isMixed = false;
        for (int i = 0; i < geom.getNumGeometries(); i++) {
            LineString line = (LineString) geom.getGeometryN(i);
            if (line.isEmpty()) continue;

            Geometry fix = fixLineStringElement(line);
            if (fix == null) continue;

            if (! (fix instanceof LineString)) {
                isMixed = true;
            }
            fixed.add(fix);
        }
        if (fixed.size() == 1) {
            return fixed.get(0);
        }
        if (isMixed) {
            return factory.createGeometryCollection(GeometryFactory.toGeometryArray(fixed));
        }
        return factory.createMultiLineString(GeometryFactory.toLineStringArray(fixed));
    }

    private Geometry fixPolygon(Polygon geom) {
        Geometry fix = fixPolygonElement(geom);
        if (fix == null)
            return factory.createPolygon();
        return fix;
    }

    private Geometry fixPolygonElement(Polygon geom) {
        LinearRing shell = geom.getExteriorRing();
        Geometry fixShell = fixRing(shell);
        if (fixShell.isEmpty()) {
            if (isKeepCollapsed) {
                return fixLineString(shell);
            }
            //-- if not allowing collapses then return empty polygon
            return null;
        }
        // if no holes then done
        if (geom.getNumInteriorRing() == 0) {
            return fixShell;
        }
        Geometry fixHoles = fixHoles(geom);
        Geometry result = removeHoles(fixShell, fixHoles);
        return result;
    }

    private Geometry removeHoles(Geometry shell, Geometry holes) {
        if (holes == null)
            return shell;
        return OverlayNGRobust.overlay(shell, holes, OverlayNG.DIFFERENCE);
    }

    private Geometry fixHoles(Polygon geom) {
        List<Geometry> holes = new ArrayList<Geometry>();
        for (int i = 0; i < geom.getNumInteriorRing(); i++) {
            Geometry holeRep = fixRing(geom.getInteriorRingN(i));
            if (holeRep != null) {
                holes.add(holeRep);
            }
        }
        if (holes.size() == 0) return null;
        if (holes.size() == 1) {
            return holes.get(0);
        }
        Geometry holesUnion = OverlayNGRobust.union(holes);
        return holesUnion;
    }

    private Geometry fixRing(LinearRing ring) {
        //-- always execute fix, since it may remove repeated coords etc
        Geometry poly = factory.createPolygon(ring);
        // TOD: check if buffer removes invalid coordinates
        return bufferByZero(poly, true);
    }

    private Geometry fixMultiPolygon(MultiPolygon geom) {
        List<Geometry> polys = new ArrayList<Geometry>();
        for (int i = 0; i < geom.getNumGeometries(); i++) {
            Polygon poly = (Polygon) geom.getGeometryN(i);
            Geometry polyFix = fixPolygonElement(poly);
            if (polyFix != null && ! polyFix.isEmpty()) {
                polys.add(polyFix);
            }
        }
        if (polys.size() == 0) {
            return factory.createMultiPolygon();
        }
        Geometry result = OverlayNGRobust.union(polys);
        return result;
    }

    private Geometry fixCollection(GeometryCollection geom) {
        Geometry[] geomRep = new Geometry[geom.getNumGeometries()];
        for (int i = 0; i < geom.getNumGeometries(); i++) {
            geomRep[i] = fix(geom.getGeometryN(i));
        }
        return factory.createGeometryCollection(geomRep);
    }

    // TEMP

    /**
     * Tests if the coordinate has valid X and Y ordinate values.
     * An ordinate value is valid iff it is finite.
     *
     * @return true if the coordinate is valid
     * @see Double#isFinite(double)
     */
    private static boolean isValid(Coordinate coord) {
        if (! Double.isFinite(coord.x)) return false;
        if (! Double.isFinite(coord.y)) return false;
        return true;
    }

    /**
     * Tests whether an array has any repeated or invalid coordinates.
     *
     * @param coord an array of coordinates
     * @return true if the array contains repeated or invalid coordinates
     */
    public static boolean hasRepeatedOrInvalid(Coordinate[] coord) {
        for (int i = 1; i < coord.length; i++) {
            if (! isValid(coord[i]))
                return true;
            if (coord[i - 1].equals(coord[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * If the coordinate array argument has repeated or invalid points,
     * constructs a new array containing no repeated points.
     * Otherwise, returns the argument.
     *
     * @param coord an array of coordinates
     * @return the array with repeated and invalid coordinates removed
     * @see #hasRepeatedOrInvalid(Coordinate[])
     */
    public static Coordinate[] removeRepeatedAndInvalidPoints(Coordinate[] coord) {
        if (!hasRepeatedOrInvalid(coord)) return coord;
        CoordinateList coordList = new CoordinateList();
        for (int i = 0; i < coord.length; i++) {
            if (! isValid(coord[i])) continue;
            coordList.add(coord[i], false);
        }
        return coordList.toCoordinateArray();
    }

    /**
     * Buffers a geometry with distance zero.
     * The result can be computed using the maximum-signed-area orientation,
     * or by combining both orientations.
     * <p>
     * This can be used to fix an invalid polygonal geometry to be valid
     * (i.e. with no self-intersections).
     * For some uses (e.g. fixing the result of a simplification)
     * a better result is produced by using only the max-area orientation.
     * Other uses (e.g. fixing geometry) require both orientations to be used.
     * <p>
     * This function is for INTERNAL use only.
     *
     * @param geom the polygonal geometry to buffer by zero
     * @param isBothOrientations true if both orientations of input rings should be used
     * @return the buffered polygonal geometry
     */
    public static Geometry bufferByZero(Geometry geom, boolean isBothOrientations) {
        //--- compute buffer using maximum signed-area orientation
        Geometry buf0 = geom.buffer(0);
        if (! isBothOrientations) return buf0;

        //-- compute buffer using minimum signed-area orientation
        BufferOp op = new BufferOp(geom);
        //op.isInvertOrientation = true;
        Geometry buf0Inv = op.getResultGeometry(0);

        //-- the buffer results should be non-adjacent, so combining is safe
        return combine(buf0, buf0Inv);
    }

    /**
     * Combines the elements of two polygonal geometries together.
     * The input geometries must be non-adjacent, to avoid
     * creating an invalid result.
     *
     * @param poly0 a polygonal geometry (which may be empty)
     * @param poly1 a polygonal geometry (which may be empty)
     * @return a combined polygonal geometry
     */
    private static Geometry combine(Geometry poly0, Geometry poly1) {
        // short-circuit - handles case where geometry is valid
        if (poly1.isEmpty()) return poly0;
        if (poly0.isEmpty()) return poly1;

        List<Polygon> polys = new ArrayList<Polygon>();
        extractPolygons(poly0, polys);
        extractPolygons(poly1, polys);
        if (polys.size() == 1) return polys.get(0);
        return poly0.getFactory().createMultiPolygon(GeometryFactory.toPolygonArray(polys));
    }

    private static void extractPolygons(Geometry poly0, List<Polygon> polys) {
        for (int i = 0; i < poly0.getNumGeometries(); i++) {
            polys.add((Polygon) poly0.getGeometryN(i));
        }
    }
}