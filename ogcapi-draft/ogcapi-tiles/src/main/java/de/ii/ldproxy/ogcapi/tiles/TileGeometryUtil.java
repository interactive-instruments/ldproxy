package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.collect.ImmutableList;
import org.locationtech.jts.algorithm.LineIntersector;
import org.locationtech.jts.algorithm.RobustLineIntersector;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.IntersectionMatrix;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    static private List<LineString> splitLineStringOnePass(LineString lineString, LineString otherLineString) {
        GeometryFactory factory = lineString.getFactory();
        ImmutableList.Builder<LineString> result = ImmutableList.builder();
        Coordinate[] newCoords = new Coordinate[lineString.getNumPoints()];
        Coordinate[] coordsOther = otherLineString.getCoordinates();
        Coordinate coord;
        int indexL1 = 0;
        for (Coordinate coordL1 : lineString.getCoordinates()) {
            if (indexL1>0) {
                if (coordL1.equals2D(newCoords[indexL1-1]))
                    continue;
                for (int indexL2 = 1; indexL2 < coordsOther.length; indexL2++) {
                    LineIntersector lineIntersector = new RobustLineIntersector();
                    lineIntersector.setPrecisionModel(lineString.getPrecisionModel());
                    lineIntersector.computeIntersection(newCoords[indexL1-1], coordL1, coordsOther[indexL2-1], coordsOther[indexL2]);
                    if (lineIntersector.hasIntersection()) {
                        // the two segments intersect, split the input line string at the intersection point,
                        // which may be the start or end point or a proper interior point; note that the
                        // segments may also be collinear
                        if (lineIntersector.getIntersectionNum()==2) {
                            double distance1 = lineIntersector.getEdgeDistance(0, 0);
                            double distance2 = lineIntersector.getEdgeDistance(0, 1);
                            int first = (distance1<distance2) ? 0 : 1;
                            int second = (distance1<distance2) ? 1 : 0;

                            coord = lineIntersector.getIntersection(first);
                            if (!coord.equals2D(newCoords[indexL1-1]))
                                newCoords[indexL1++] = coord;
                            if (indexL1 > 1) {
                                LineString newLineString = factory.createLineString(Arrays.copyOf(newCoords, indexL1));
                                result.add(newLineString);
                                newCoords[0] = coord;
                                indexL1 = 1;
                            }

                            coord = lineIntersector.getIntersection(second);
                            if (!coord.equals2D(newCoords[indexL1-1]))
                                newCoords[indexL1++] = coord;
                            if (indexL1 > 1) {
                                LineString newLineString = factory.createLineString(Arrays.copyOf(newCoords, indexL1));
                                result.add(newLineString);
                                newCoords[0] = coord;
                                indexL1 = 1;
                            }
                        } else {
                            coord = lineIntersector.getIntersection(0);
                            if (!coord.equals2D(newCoords[indexL1-1]))
                                newCoords[indexL1++] = coord;
                            if (indexL1 > 1) {
                                LineString newLineString = factory.createLineString(Arrays.copyOf(newCoords, indexL1));
                                result.add(newLineString);
                                newCoords[0] = coord;
                                indexL1 = 1;
                            }
                        }
                    }
                }
                // all segments of the other line string processed, add the coordinate
                if (!coordL1.equals2D(newCoords[indexL1-1]))
                    newCoords[indexL1++] = coordL1;
            } else {
                newCoords[indexL1++] = coordL1;
            }
        }
        if (indexL1 > 1)
            result.add(factory.createLineString(Arrays.copyOf(newCoords, indexL1)));

        return result.build();
    }

    static private List<LineString> splitLineString(LineString lineString, LineString otherLineString, int level) {
        // LOGGER.debug("Call level {}: Splitting line strings {} and {}.", level, lineString, otherLineString);

        List<LineString> result = splitLineStringOnePass(lineString, otherLineString);
        if (result.size()==1)
            return result;

        return result.stream()
                     .map(ls -> splitLineString(ls, otherLineString, level+1))
                     .flatMap(Collection::stream)
                     .collect(Collectors.toList());
    }

    // TODO optimize performance, minimize memory
    static MultiPolygon rebuildPolygon(Geometry geom, GeometryPrecisionReducer reducer) {
        LOGGER.debug("Rebuilding an invalid polygon geometry with {} vertices.", geom.getNumPoints());
        LineMerger lineMerger = new LineMerger();
        lineMerger.add(geom);
        Set<LineString> edges = (Set<LineString>) lineMerger.getMergedLineStrings().stream().collect(Collectors.toSet());
        LineString[] lineStringsOrig = edges.toArray(LineString[]::new);
        for (LineString lineString1 : lineStringsOrig) {
            boolean cont = true;
            while (cont) {
                LineString[] edgesCopy = edges.toArray(LineString[]::new);
                edges.clear();
                cont = false;
                for (LineString lineString2 : edgesCopy) {
                    if (lineString1.equals(lineString2)) {
                        edges.add(lineString2);
                        continue;
                    }
                    IntersectionMatrix matrix = lineString1.relate(lineString2);
                    if (matrix.matches("FF*F*****")) {
                        edges.add(lineString2);
                    } else {
                        List<LineString> result = splitLineString(lineString2, lineString1, 1);
                        edges.addAll(result);
                        cont = result.size() > 1;
                    }
                }
            }
        }
        Polygonizer polygonizer = new Polygonizer(true);
        List<Geometry> lineStringsOnGrid = edges.stream().map(edge -> reducer.reduce(edge)).collect(Collectors.toList());
        polygonizer.add(lineStringsOnGrid);
        Collection polygons = polygonizer.getPolygons();
        if (polygons.isEmpty())
            return null;
        return geom.getFactory().createMultiPolygon((Polygon[]) polygons.toArray(Polygon[]::new));
    }

    static Geometry processPolygons(Geometry geom, GeometryPrecisionReducer reducer, double distanceTolerance) {
        boolean geomUpdated = false;
        if (geom instanceof Polygon || geom instanceof MultiPolygon) {
            // the standard fix for invalid, self-intersecting polygons is to use a zero-distance buffer;
            // however, this does not work in all cases and sometimes creates invalid or unwanted results
            Geometry bufferGeom = geom.buffer(0.0);
            double areaChange = Math.abs(bufferGeom.getArea() - geom.getArea()) / geom.getArea();
            if (areaChange > 0.5 || !bufferGeom.isValid()) {
                // if the deviation is too big or invalid, try a union
                Geometry unionGeom = geom.union();
                areaChange = Math.abs(unionGeom.getArea() - geom.getArea()) / geom.getArea();
                if (areaChange > 0.5 || !unionGeom.isValid()) {
                    // if the deviation is too big or invalid, try a convex hull
                    Geometry hullGeom = geom.convexHull();
                    areaChange = Math.abs(hullGeom.getArea() - geom.getArea()) / geom.getArea();
                    if (areaChange <= 0.5 && hullGeom.isValid()) {
                        geom = hullGeom;
                        geomUpdated = true;
                    } else {
                        Geometry newGeom = rebuildPolygon(unionGeom, reducer);
                        if (Objects.nonNull(newGeom)) {
                            LOGGER.debug("Polygon rebuilt, valid={}, old area {}, new area {}.", newGeom.isValid(), unionGeom.getArea(), newGeom.getArea());

                            if (newGeom.isValid()) {
                                // simplify the geometry, if the geometry hasn't been simplified before
                                // if (distanceTolerance != Double.NaN)
                                //    newGeom = TopologyPreservingSimplifier.simplify(newGeom, distanceTolerance);

                                // remove small rings or line strings (small in the context of the tile) that may
                                // have been created in some cases by changing to the tile grid
                                newGeom = TileGeometryUtil.removeSmallPieces(newGeom);
                                if (newGeom == null || newGeom.isEmpty()) {
                                    LOGGER.debug("Empty result after removing small pieces.");
                                    return null;
                                }
                                LOGGER.debug("Small pieces removed, valid={}, new area {}.", newGeom.isValid(), newGeom.getArea());

                                // make sure the geometry is using the tile grid, if we processed a polygon geometry
                                newGeom = reducer.reduce(newGeom);
                                LOGGER.debug("Reduced to grid, valid={}, new area {}.", newGeom.isValid(), newGeom.getArea());

                                if (newGeom.isValid())
                                    return newGeom;
                            } else {
                                LOGGER.debug("Polygon rebuild failed.");
                            }

                        } else {
                            LOGGER.debug("Polygon rebuilt with empty result");
                        }
                    }
                } else {
                    geom = unionGeom;
                    geomUpdated = true;
                }
            } else {
                geom = bufferGeom;
                geomUpdated = true;
            }

            // simplify the geometry, if the geometry hasn't been simplified before
            if (geomUpdated && distanceTolerance != Double.NaN)
                geom = TopologyPreservingSimplifier.simplify(geom, distanceTolerance);
        }

        // remove small rings or line strings (small in the context of the tile) that may
        // have been created in some cases by changing to the tile grid
        geom = TileGeometryUtil.removeSmallPieces(geom);
        if (geom==null || geom.isEmpty()) {
            return null;
        }

        // make sure the geometry is using the tile grid, if we processed a polygon geometry
        if (geomUpdated)
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


