/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.filtertransformer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.json.simple.parser.JSONParser;
import org.locationtech.jts.geom.CoordinateSequence;
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
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.io.ParseException;

public class GeoJsonReader {
    private GeometryFactory gf;

    public GeoJsonReader() {
    }

    public GeoJsonReader(GeometryFactory geometryFactory) {
        this.gf = geometryFactory;
    }

    public Geometry read(String json) throws ParseException {
        Geometry result = this.read((Reader) (new StringReader(json)));
        return result;
    }

    public Geometry read(Reader reader) throws ParseException {
        Geometry result = null;
        JSONParser parser = new JSONParser();

        try {
            Map<String, Object> geometryMap = (Map) parser.parse(reader);
            GeometryFactory geometryFactory = null;
            if (this.gf == null) {
                geometryFactory = this.getGeometryFactory(geometryMap);
            } else {
                geometryFactory = this.gf;
            }

            result = this.create(geometryMap, geometryFactory);
            return result;
        } catch (org.json.simple.parser.ParseException var6) {
            throw new ParseException(var6);
        } catch (IOException var7) {
            throw new ParseException(var7);
        }
    }

    private Geometry create(Map<String, Object> geometryMap, GeometryFactory geometryFactory) throws ParseException {
        Geometry result = null;
        String type = (String) geometryMap.get("type");
        if (type == null) {
            throw new ParseException("Could not parse Geometry from Json string.  No 'type' property found.");
        } else {
            if ("Feature".equals(type) && geometryMap.containsKey("geometry")) {
                result = create((Map<String, Object>) geometryMap.get("geometry"), geometryFactory);
            } else if ("FeatureCollection".equals(type) && geometryMap.containsKey("features")) {
                List<Map<String, Object>> feturesList = (List) geometryMap.get("features");
                List<Map<String, Object>> geometries = feturesList.stream()
                                                                  .filter(feature -> feature.containsKey("geometry"))
                                                                  .map(feature -> (Map<String, Object>) feature.get("geometry"))
                                                                  .collect(Collectors.toList());
                result =  createGeometryCollection(ImmutableMap.of("geometries", geometries), geometryFactory);
            } else if ("Point".equals(type)) {
                result = this.createPoint(geometryMap, geometryFactory);
            } else if ("LineString".equals(type)) {
                result = this.createLineString(geometryMap, geometryFactory);
            } else if ("Polygon".equals(type)) {
                result = this.createPolygon(geometryMap, geometryFactory);
            } else if ("MultiPoint".equals(type)) {
                result = this.createMultiPoint(geometryMap, geometryFactory);
            } else if ("MultiLineString".equals(type)) {
                result = this.createMultiLineString(geometryMap, geometryFactory);
            } else if ("MultiPolygon".equals(type)) {
                result = this.createMultiPolygon(geometryMap, geometryFactory);
            } else {
                if (!"GeometryCollection".equals(type)) {
                    throw new ParseException("Could not parse Geometry from GeoJson string.  Unsupported 'type':" + type);
                }

                result = this.createGeometryCollection(geometryMap, geometryFactory);
            }

            return result;
        }
    }

    private Geometry createGeometryCollection(Map<String, Object> geometryMap, GeometryFactory geometryFactory) throws ParseException {
        GeometryCollection result = null;

        try {
            List<Map<String, Object>> geometriesList = (List) geometryMap.get("geometries");
            Geometry[] geometries = new Geometry[geometriesList.size()];
            int i = 0;

            for (Iterator var7 = geometriesList.iterator(); var7.hasNext(); ++i) {
                Map<String, Object> map = (Map) var7.next();
                geometries[i] = this.create(map, geometryFactory);
            }

            result = geometryFactory.createGeometryCollection(geometries);
            return result;
        } catch (RuntimeException var9) {
            throw new ParseException("Could not parse GeometryCollection from GeoJson string.", var9);
        }
    }

    private Geometry createMultiPolygon(Map<String, Object> geometryMap, GeometryFactory geometryFactory) throws ParseException {
        MultiPolygon result = null;

        try {
            List<List<List<List<Number>>>> polygonsList = (List) geometryMap.get("coordinates");
            Polygon[] polygons = new Polygon[polygonsList.size()];
            int p = 0;
            Iterator var7 = polygonsList.iterator();

            while (true) {
                ArrayList rings;
                do {
                    if (!var7.hasNext()) {
                        result = geometryFactory.createMultiPolygon(polygons);
                        return result;
                    }

                    List<List<List<Number>>> ringsList = (List) var7.next();
                    rings = new ArrayList();
                    Iterator var10 = ringsList.iterator();

                    while (var10.hasNext()) {
                        List<List<Number>> coordinates = (List) var10.next();
                        rings.add(this.createCoordinateSequence(coordinates));
                    }
                } while (rings.isEmpty());

                LinearRing outer = geometryFactory.createLinearRing((CoordinateSequence) rings.get(0));
                LinearRing[] inner = null;
                if (rings.size() > 1) {
                    inner = new LinearRing[rings.size() - 1];

                    for (int i = 1; i < rings.size(); ++i) {
                        inner[i - 1] = geometryFactory.createLinearRing((CoordinateSequence) rings.get(i));
                    }
                }

                polygons[p] = geometryFactory.createPolygon(outer, inner);
                ++p;
            }
        } catch (RuntimeException var13) {
            throw new ParseException("Could not parse MultiPolygon from GeoJson string.", var13);
        }
    }

    private Geometry createMultiLineString(Map<String, Object> geometryMap, GeometryFactory geometryFactory) throws ParseException {
        MultiLineString result = null;

        try {
            List<List<List<Number>>> linesList = (List) geometryMap.get("coordinates");
            LineString[] lineStrings = new LineString[linesList.size()];
            int i = 0;

            for (Iterator var7 = linesList.iterator(); var7.hasNext(); ++i) {
                List<List<Number>> coordinates = (List) var7.next();
                lineStrings[i] = geometryFactory.createLineString(this.createCoordinateSequence(coordinates));
            }

            result = geometryFactory.createMultiLineString(lineStrings);
            return result;
        } catch (RuntimeException var9) {
            throw new ParseException("Could not parse MultiLineString from GeoJson string.", var9);
        }
    }

    private Geometry createMultiPoint(Map<String, Object> geometryMap, GeometryFactory geometryFactory) throws ParseException {
        MultiPoint result = null;

        try {
            List<List<Number>> coordinatesList = (List) geometryMap.get("coordinates");
            CoordinateSequence coordinates = this.createCoordinateSequence(coordinatesList);
            result = geometryFactory.createMultiPoint(coordinates);
            return result;
        } catch (RuntimeException var6) {
            throw new ParseException("Could not parse MultiPoint from GeoJson string.", var6);
        }
    }

    private Geometry createPolygon(Map<String, Object> geometryMap, GeometryFactory geometryFactory) throws ParseException {
        Polygon result = null;

        try {
            List<List<List<Number>>> ringsList = (List) geometryMap.get("coordinates");
            List<CoordinateSequence> rings = new ArrayList();
            Iterator var6 = ringsList.iterator();

            while (var6.hasNext()) {
                List<List<Number>> coordinates = (List) var6.next();
                rings.add(this.createCoordinateSequence(coordinates));
            }

            if (rings.isEmpty()) {
                throw new IllegalArgumentException("Polygon specified with no rings.");
            } else {
                LinearRing outer = geometryFactory.createLinearRing((CoordinateSequence) rings.get(0));
                LinearRing[] inner = null;
                if (rings.size() > 1) {
                    inner = new LinearRing[rings.size() - 1];

                    for (int i = 1; i < rings.size(); ++i) {
                        inner[i - 1] = geometryFactory.createLinearRing((CoordinateSequence) rings.get(i));
                    }
                }

                result = geometryFactory.createPolygon(outer, inner);
                return result;
            }
        } catch (RuntimeException var9) {
            throw new ParseException("Could not parse Polygon from GeoJson string.", var9);
        }
    }

    private Geometry createLineString(Map<String, Object> geometryMap, GeometryFactory geometryFactory) throws ParseException {
        LineString result = null;

        try {
            List<List<Number>> coordinatesList = (List) geometryMap.get("coordinates");
            CoordinateSequence coordinates = this.createCoordinateSequence(coordinatesList);
            result = geometryFactory.createLineString(coordinates);
            return result;
        } catch (RuntimeException var6) {
            throw new ParseException("Could not parse LineString from GeoJson string.", var6);
        }
    }

    private Geometry createPoint(Map<String, Object> geometryMap, GeometryFactory geometryFactory) throws ParseException {
        Point result = null;

        try {
            List<Number> coordinateList = (List) geometryMap.get("coordinates");
            CoordinateSequence coordinate = this.createCoordinate(coordinateList);
            result = geometryFactory.createPoint(coordinate);
            return result;
        } catch (RuntimeException var6) {
            throw new ParseException("Could not parse Point from GeoJson string.", var6);
        }
    }

    private GeometryFactory getGeometryFactory(Map<String, Object> geometryMap) throws ParseException {
        GeometryFactory result = null;
        Map<String, Object> crsMap = (Map) geometryMap.get("crs");
        Integer srid = null;
        if (crsMap != null) {
            try {
                Map<String, Object> propertiesMap = (Map) crsMap.get("properties");
                String name = (String) propertiesMap.get("name");
                String[] split = name.split(":");
                String epsg = split[1];
                srid = Integer.valueOf(epsg);
            } catch (RuntimeException var9) {
                throw new ParseException("Could not parse SRID from Geojson 'crs' object.", var9);
            }
        }

        if (srid == null) {
            srid = 4326;
        }

        result = new GeometryFactory(new PrecisionModel(), srid);
        return result;
    }

    private CoordinateSequence createCoordinateSequence(List<List<Number>> coordinates) {
        CoordinateSequence result = null;
        result = new CoordinateArraySequence(coordinates.size());

        for (int i = 0; i < coordinates.size(); ++i) {
            List<Number> ordinates = (List) coordinates.get(i);
            if (ordinates.size() > 0) {
                result.setOrdinate(i, 0, ((Number) ordinates.get(0)).doubleValue());
            }

            if (ordinates.size() > 1) {
                result.setOrdinate(i, 1, ((Number) ordinates.get(1)).doubleValue());
            }

            if (ordinates.size() > 2) {
                result.setOrdinate(i, 2, ((Number) ordinates.get(2)).doubleValue());
            }
        }

        return result;
    }

    private CoordinateSequence createCoordinate(List<Number> ordinates) {
        CoordinateSequence result = new CoordinateArraySequence(1);
        if (ordinates.size() > 0) {
            result.setOrdinate(0, 0, ((Number) ordinates.get(0)).doubleValue());
        }

        if (ordinates.size() > 1) {
            result.setOrdinate(0, 1, ((Number) ordinates.get(1)).doubleValue());
        }

        if (ordinates.size() > 2) {
            result.setOrdinate(0, 2, ((Number) ordinates.get(2)).doubleValue());
        }

        return result;
    }
}