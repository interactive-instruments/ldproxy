/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.ldproxy.gml2json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import de.ii.ldproxy.output.geojson.GeoJsonGeometryMapping;
import de.ii.ldproxy.output.geojson.GeoJsonPropertyMapping;
import static de.ii.ldproxy.output.geojson.GeoJsonMapping.GEO_JSON_TYPE;
import static de.ii.ldproxy.output.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import static de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;
import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeMapping;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.ogc.api.exceptions.GMLAnalyzeFailed;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLAnalyzer;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMInputCursor;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 *
 * @author zahnen
 */
public abstract class AbstractFeatureWriter implements GMLAnalyzer {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(AbstractFeatureWriter.class);
    protected JsonGenerator json;
    protected JsonGenerator jsonOut;
    protected TokenBuffer jsonBuffer;
    protected ObjectMapper jsonMapper;
    //protected WFS2GSFSLayer layer;
    protected XMLPathTracker currentPath;
    protected EpsgCrs outSRS;
    protected List<String> outFields;
    protected boolean geometryWithSR;
    protected boolean returnGeometry;
    protected Map<String, Integer> fieldCounter;
    protected boolean isFeatureCollection;
    protected double maxAllowableOffset;
    protected CrsTransformer crsTransformer;

    protected String outputFormat; // as constant somewhere
    protected WfsProxyFeatureTypeMapping featureTypeMapping; // reduceToOutputFormat
    // type
    // id
    // properties
    // geometry


    public AbstractFeatureWriter(/*WFS2GSFSLayer layer,*/ JsonGenerator jsonOut, ObjectMapper jsonMapper, boolean isFeatureCollection /*,LayerQueryParameters layerQueryParams, boolean featureRessource*/, CrsTransformer crsTransformer) {
        this.jsonOut = jsonOut;
        this.json = jsonOut;
        this.jsonMapper = jsonMapper;
        this.isFeatureCollection = isFeatureCollection;
        //this.layer = layer;
        //this.featureRessource = featureRessource;
        //this.returnGeometry = layerQueryParams.isReturnGeometry();
        this.currentPath = new XMLPathTracker();
        /*if (layerQueryParams.getOutSR() != null) {
            this.outSRS = layerQueryParams.getOutSR();
        } else {
            this.outSRS = layer.getWfs().getDefaultSR();
        }
        if (layerQueryParams.getOutFields() != null) {
            this.outFields = Arrays.asList(layerQueryParams.getOutFields());
        } else {
            this.outFields = new ArrayList<>();
        }
        this.geometryWithSR = false;
        this.maxAllowableOffset = layerQueryParams.getMaxAllowableOffset();
        */
        this.crsTransformer = crsTransformer;
    }

    @Override
    public final void analyzeFailed(Exception ex) {
        throw new GMLAnalyzeFailed("AbstractFeatureWriter -> analyzeFailed");
    }

    @Override
    public final void analyzeStart(Future<SMInputCursor> rootFuture) {
        try {
            writeStart(rootFuture);
        } catch (IOException ex) {
            //LOGGER.debug(FrameworkMessages.ERROR_WRITING_RESPONSE_CLIENT_GONE, ex.getMessage());
            analyzeFailed(ex);
        }
    }

    @Override
    public void analyzeFeatureStart(String id, String nsuri, String localName) {
        fieldCounter = new HashMap<>();
        currentPath.clear();
        try {
            writeFeatureStart();
        } catch (IOException ex) {
            //LOGGER.debug(FrameworkMessages.ERROR_WRITING_RESPONSE_CLIENT_GONE, ex.getMessage());
            analyzeFailed(ex);
        }
    }

    @Override
    public final void analyzeAttribute(String nsuri, String localName, String value) {
        currentPath.track(nsuri, "@" + localName);
        String path = currentPath.toString();
        //LOGGER.debug(" - attribute {}", path);

        for (TargetMapping mapping: featureTypeMapping.findMappings(path, outputFormat)) {
            if (!mapping.isEnabled()) {
                continue;
            }

            writeField((GeoJsonPropertyMapping)mapping, value);
        }
    }

    @Override
    public final void analyzePropertyStart(String nsuri, String localName, int depth, SMInputCursor feature, boolean nil) {
        currentPath.track(nsuri, localName, depth);
        String path = currentPath.toString();
        String value = "";

        for (TargetMapping mapping: featureTypeMapping.findMappings(path, outputFormat)) {
            if (!mapping.isEnabled()) {
                continue;
            }

            // TODO: I guess fieldCounter is for multiplicity

            // TODO: only if not geometry
            if (((GeoJsonPropertyMapping)mapping).getType() != GEO_JSON_TYPE.GEOMETRY) {
                if (value.isEmpty()) {
                    try {
                        value = feature.collectDescendantText();
                    } catch (XMLStreamException ex) {
                        //LOGGER.error(FrameworkMessages.ERROR_PARSING_GETFEATURE_REQUEST);
                        analyzeFailed(ex);
                    }
                }

                writeField((GeoJsonPropertyMapping) mapping, value);
            } else {
                // TODO: write geometry
                writeGeometry(((GeoJsonGeometryMapping)mapping).getGeometryType(), feature);
            }
        }
    }

    @Override
    public void analyzePropertyEnd(String nsuri, String localName, int depth) {
    }

    @Override
    public final void analyzeFeatureEnd() {

        try {
            stopBuffering();
            flushBuffer();
        } catch (IOException ex) {
            analyzeFailed(ex);
        }

        try {
            writeFeatureEnd();
        } catch (IOException ex) {
            //LOGGER.debug(FrameworkMessages.ERROR_WRITING_RESPONSE_CLIENT_GONE, ex.getMessage());
            analyzeFailed(ex);
        }
    }

    @Override
    public final void analyzeEnd() {
        try {
            writeEnd();
        } catch (IOException ex) {
            //LOGGER.debug(FrameworkMessages.ERROR_WRITING_RESPONSE_CLIENT_GONE, ex.getMessage());
            analyzeFailed(ex);
        }
    }

    protected abstract void writeStart(SMInputCursor rootFuture) throws IOException;
    
    protected abstract void writeStart(Future<SMInputCursor> rootFuture) throws IOException;

    protected abstract void writeFeatureStart() throws IOException;

    protected abstract void writeStartProperties() throws IOException;

    protected abstract void writeFeatureEnd() throws IOException;

    protected void writeField(GeoJsonPropertyMapping mapping, String value) {

        if (value == null || value.isEmpty()) {
            return;
        }

        try {
            switch (mapping.getType()) {
                case STRING:
                    json.writeStringField(mapping.getName(), value);
                    break;
                case ID:
                    json.writeStringField(mapping.getName(), value);
                    writeStartProperties();
                    break;
                case NUMBER:
                    // TODO: howto recognize int or double
                    try {
                        json.writeNumberField(mapping.getName(), Long.parseLong(value));
                    } catch (NumberFormatException ex) {
                        json.writeNumberField(mapping.getName(), Long.parseLong(value.split(".")[0]));
                    }
                    break;
            }
        } catch (NumberFormatException ex) {
            //LOGGER.error(FrameworkMessages.NUMBERFORMATEXCEPTION_WRITEMAPPEDFIELD, type.toString(), value);
            analyzeFailed(ex);
        } catch (IOException ex) {
            //LOGGER.error(FrameworkMessages.ERROR_WRITING_RESPONSE_WRITEMAPPEDFIELD, ex.getMessage(), json.getOutputContext().getCurrentName(), value);
            analyzeFailed(ex);
        }
    }

    protected abstract void writeGeometryHeader(GEO_JSON_GEOMETRY_TYPE type) throws IOException;

    protected abstract void writePointGeometry(String x, String y) throws IOException;

    private void writeGeometry(GEO_JSON_GEOMETRY_TYPE type, SMInputCursor feature) {
        GML_GEOMETRY_TYPE gmlType = GML_GEOMETRY_TYPE.NONE;

        try {
            stopBuffering();

            json.writeObjectFieldStart("geometry");

            Boolean multiContext = false;
            int srsDimension = 2;

            CoordinatesWriterType.Builder cwBuilder = CoordinatesWriterType.builder(json);

            if (crsTransformer != null) {
                cwBuilder.transformer(crsTransformer);
            }
            /*CrsTransformer transformer = null;
            if (!layer.supportsSrs(outSRS)) {
                // TODO
                // workaround for AGOL requesting 102100
                SpatialReference outSRSwa = new SpatialReference(3857);
                if (outSRS.getWkid() == 102100 && layer.supportsSrs(outSRSwa)) {

                } else {
                    transformer = layer.getSrsTransformations().getOutputTransformer(outSRS);
                    cwBuilder.transformer(transformer);
                }
            }
            if (layer.getSrsTransformations().mustReverseOutputAxisOrder(outSRS)) {
                cwBuilder.swap();
            }
            if (type == Layer.GEOMETRY_TYPE.POLYGON && layer.getMustReversePolygon()) {
                cwBuilder.reversepolygon();
            }
            if (layer.supportsMaxAllowableOffset() && maxAllowableOffset > 0) {
                cwBuilder.simplifier(new DouglasPeuckerLineSimplifier(
                        layer.getSrsTransformations().normalizeMaxAllowableOffset(maxAllowableOffset, outSRS)));
            }*/

            // TODO: MultiPolygon needs one more bracket
            SMInputCursor geo = feature.descendantElementCursor().advance();
            while (geo.readerAccessible()) {
                if (!gmlType.isValid()) {
                    GML_GEOMETRY_TYPE nodeType = GML_GEOMETRY_TYPE.fromString(geo.getLocalName());
                    if (nodeType.isValid()) {
                        gmlType = nodeType;
                        if (geo.getAttrValue("srsDimension") != null) {
                            srsDimension = Integer.valueOf(geo.getAttrValue("srsDimension"));
                            cwBuilder.dimension(srsDimension);
                        }
                    }
                } 
                
                if (geo.getCurrEvent().equals(SMEvent.START_ELEMENT)
                        && (geo.hasLocalName("exterior") || geo.hasLocalName("interior") 
                        || geo.hasLocalName("outerBoundaryIs") || geo.hasLocalName("innerBoundaryIs")
                        || geo.hasLocalName("LineString"))) {

                    if (!multiContext) {
                        this.writeGeometryHeader(type);
                        // TODO
                        json.writeStartArray();
                        json.writeStartArray();
                        multiContext = true;
                    }
                    json.writeStartArray();

                } else if (geo.getCurrEvent().equals(SMEvent.END_ELEMENT)
                        && (geo.hasLocalName("exterior") || geo.hasLocalName("interior")
                        || geo.hasLocalName("outerBoundaryIs") || geo.hasLocalName("innerBoundaryIs")
                        || geo.hasLocalName("LineString"))) {

                    json.writeEndArray();

                } else if (geo.hasLocalName("posList") || geo.hasLocalName("pos") || geo.hasLocalName("coordinates")) {
                    if (type == GEO_JSON_GEOMETRY_TYPE.POLYGON || type == GEO_JSON_GEOMETRY_TYPE.LINE_STRING || type == GEO_JSON_GEOMETRY_TYPE.MULTI_LINE_STRING || type == GEO_JSON_GEOMETRY_TYPE.MULTI_POLYGON) {

                        Writer coordinatesWriter = cwBuilder.build();
                        try {
                            geo.processDescendantText(coordinatesWriter, false);
                            coordinatesWriter.close();
                        } catch (IOException ex) {
                        }

                    } else if (type == GEO_JSON_GEOMETRY_TYPE.POINT) {
                        String kord = geo.getElemStringValue();
                        String[] korda = kord.split(" ");

                        /*if (layer.getWfs().getVersion().equals(_1_0_0.toString())) {
                            korda = kord.split(","); // WFS 1.0.0 ...                            
                        }*/

                        CoordinateTuple point = new CoordinateTuple(korda[0], korda[1]);

                        if (crsTransformer != null) {
                            point = crsTransformer.transform(point);
                        } /*else if (layer.getSrsTransformations().mustReverseOutputAxisOrder(outSRS)) {
                            point = new CoordinateTuple(point.getY(), point.getX());
                        }*/

                        this.writePointGeometry(point.getXasString(), point.getYasString());
                    }

                }
                geo = geo.advance();

            }
            if (geometryWithSR) {
                json.writeObjectField("spatialReference", outSRS);
            }

            if (multiContext) {
                // TODO
                json.writeEndArray();
                json.writeEndArray();
            }

            json.writeEndObject();
            flushBuffer();
        } catch (XMLStreamException ex) {
            //LOGGER.debug(FrameworkMessages.ERROR_PARSING_GETFEATURE_REQUEST);
            analyzeFailed(ex);
        } catch (IOException ex) {
            //LOGGER.debug(FrameworkMessages.ERROR_WRITING_RESPONSE_WRITEGEOMETRY, ex.getMessage());
            analyzeFailed(ex);
        }
    }

    protected abstract void writeEnd() throws IOException;

    protected final void startBuffering() throws IOException {
        jsonOut.flush();
        this.jsonBuffer = createJsonBuffer();
        this.json = jsonBuffer;
    }

    protected final void stopBuffering() throws IOException {
        if (jsonBuffer != null) {
            jsonBuffer.close();
        }
        this.json = jsonOut;
    }

    protected final void flushBuffer() throws IOException {
        if (jsonBuffer != null) {
            jsonBuffer.serialize(jsonOut);
            jsonBuffer = null;
        }
    }

    // TODO
    public TokenBuffer createJsonBuffer() throws IOException {
        TokenBuffer json = new TokenBuffer(jsonMapper);

        //if (useFormattedJsonOutput) {
        json.useDefaultPrettyPrinter();
        //}
        return json;
    }

    /*enum GEOMETRY_TYPE_MAPPING {

        MULTI_SURFACE("MultiSurface", Layer.GEOMETRY_TYPE.POLYGON),
        POLYGON("Polygon", Layer.GEOMETRY_TYPE.POLYGON),
        CURVE("LineString", Layer.GEOMETRY_TYPE.POLYLINE),
        CCURVE("Curve", Layer.GEOMETRY_TYPE.POLYLINE),
        LINESTRINGSEGMENT("LineStringSegment", Layer.GEOMETRY_TYPE.POLYLINE),
        MULTI_LINESTRING("MultiLinestring", Layer.GEOMETRY_TYPE.POLYLINE),
        LINESTRING("LineString", Layer.GEOMETRY_TYPE.POLYLINE),
        MULTI_CURVE("MultiCurve", Layer.GEOMETRY_TYPE.POLYLINE),
        POINT("Point", Layer.GEOMETRY_TYPE.POINT),
        NONE("", Layer.GEOMETRY_TYPE.NONE);
        private String stringRepresentation;
        private Layer.GEOMETRY_TYPE esriType;

        private GEOMETRY_TYPE_MAPPING(String stringRepresentation, Layer.GEOMETRY_TYPE esriType) {
            this.stringRepresentation = stringRepresentation;
            this.esriType = esriType;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }

        public static GEOMETRY_TYPE_MAPPING fromString(String type) {
            for (GEOMETRY_TYPE_MAPPING v : GEOMETRY_TYPE_MAPPING.values()) {
                if (v.toString().equals(type)) {
                    return v;
                }
            }
            return NONE;
        }

        public static boolean contains(String type) {
            for (FeatureType2LayerMapper.GEOMETRY_TYPE_MAPPING v : FeatureType2LayerMapper.GEOMETRY_TYPE_MAPPING.values()) {
                if (v.toString().equals(type)) {
                    return true;
                }
            }
            return false;
        }

        public Layer.GEOMETRY_TYPE toEsri() {
            return esriType;
        }

        public boolean isValid() {
            return this != NONE;
        }
    }*/
}
