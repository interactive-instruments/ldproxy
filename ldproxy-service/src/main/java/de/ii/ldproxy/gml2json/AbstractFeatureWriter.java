/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.gml2json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeMapping;
import de.ii.ogc.wfs.proxy.WfsProxyOnTheFlyMapping;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.ogc.api.exceptions.GMLAnalyzeFailed;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLAnalyzer;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
import org.codehaus.staxmate.in.SMInputCursor;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
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

    protected WfsProxyOnTheFlyMapping onTheFlyMapping;


    public AbstractFeatureWriter(/*WFS2GSFSLayer layer,*/ JsonGenerator jsonOut, ObjectMapper jsonMapper, boolean isFeatureCollection /*,LayerQueryParameters layerQueryParams, boolean featureRessource*/, CrsTransformer crsTransformer, WfsProxyOnTheFlyMapping onTheFlyMapping) {
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
        this.onTheFlyMapping = onTheFlyMapping;
    }

    @Override
    public final void analyzeFailed(Exception ex) {
        LOGGER.getLogger().error("AbstractFeatureWriter -> analyzeFailed", ex);
        throw new GMLAnalyzeFailed("AbstractFeatureWriter -> analyzeFailed");
    }

    @Override
    public boolean analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName) {
        return false;
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

        TargetMapping featureMapping = null;

        if (featureTypeMapping.getMappings().isEmpty()) {
            featureMapping = onTheFlyMapping.getTargetMappingForFeatureType(currentPath, nsuri, localName);
        }

        for (TargetMapping mapping : featureTypeMapping.findMappings(nsuri + ":" + localName, outputFormat)) {
            if (!mapping.isEnabled()) {
                continue;
            }
            featureMapping = mapping;
            break;
        }

        try {
            writeFeatureStart(featureMapping);
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

        if (featureTypeMapping.getMappings().isEmpty()) {
            TargetMapping mapping = onTheFlyMapping.getTargetMappingForAttribute(currentPath, nsuri, localName, value);
            if (mapping != null) {
                writeField(mapping, value);
            }
            return;
        }

        for (TargetMapping mapping: featureTypeMapping.findMappings(path, outputFormat)) {
            if (!mapping.isEnabled()) {
                continue;
            }

            writeField(mapping, value);
        }
    }

    @Override
    public final void analyzePropertyStart(String nsuri, String localName, int depth, SMInputCursor feature, boolean nil) {
        currentPath.track(nsuri, localName, depth);
        String path = currentPath.toString();
        String value = "";

        if (featureTypeMapping.getMappings().isEmpty()) {
            TargetMapping mapping = onTheFlyMapping.getTargetMappingForGeometry(currentPath, nsuri, localName);

            if (mapping != null) {
                writeGeometry(mapping, feature);
            }
            return;
        }

        for (TargetMapping mapping: featureTypeMapping.findMappings(path, outputFormat)) {
            if (!mapping.isEnabled()) {
                continue;
            }

            // TODO: I guess fieldCounter is for multiplicity

            // TODO: only if not geometry
            if (!mapping.isGeometry()) {
                if (value.isEmpty()) {
                    try {
                        value = feature.collectDescendantText();
                    } catch (XMLStreamException ex) {
                        //LOGGER.error(FrameworkMessages.ERROR_PARSING_GETFEATURE_REQUEST);
                        analyzeFailed(ex);
                    }
                }

                writeField(mapping, value);
            } else {
                // TODO: write geometry
                writeGeometry(mapping, feature);
            }
        }
    }

    @Override
    public void analyzePropertyText(String nsuri, String localName, int depth, String text) {
        if (featureTypeMapping.getMappings().isEmpty()) {
            TargetMapping mapping = onTheFlyMapping.getTargetMappingForProperty(currentPath, nsuri, localName, text);
            if (mapping != null) {
                writeField(mapping, text);
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

    protected abstract void writeFeatureStart(TargetMapping mapping) throws IOException;

    protected abstract void writeStartProperties() throws IOException;

    protected abstract void writeFeatureEnd() throws IOException;

    protected abstract void writeField(TargetMapping mapping, String value);

    protected abstract void writePointGeometry(String x, String y) throws IOException;

    protected abstract void writeGeometry(TargetMapping mapping, SMInputCursor feature);

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
        TokenBuffer json = new TokenBuffer(jsonMapper, false);

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
