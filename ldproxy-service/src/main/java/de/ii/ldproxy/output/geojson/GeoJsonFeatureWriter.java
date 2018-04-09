/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.geojson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.ldproxy.gml2json.AbstractFeatureWriter;
import de.ii.ldproxy.gml2json.CoordinatesWriterType;
import de.ii.ldproxy.gml2json.JsonCoordinateFormatter;
import de.ii.ldproxy.output.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.ldproxy.wfs3.Wfs3Link;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;
import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeMapping;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMInputCursor;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fischer
 */
public class GeoJsonFeatureWriter extends AbstractFeatureWriter {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(GeoJsonFeatureWriter.class);

    private final List<Wfs3Link> links;
           
    public GeoJsonFeatureWriter(JsonGenerator jsonOut, ObjectMapper jsonMapper, boolean isFeatureCollection, WfsProxyFeatureTypeMapping featureTypeMapping, String outputFormat, CrsTransformer crsTransformer, List<Wfs3Link> links) {
        super(jsonOut, jsonMapper, isFeatureCollection, crsTransformer, new GeoJsonOnTheFlyMapping());
        this.featureTypeMapping = featureTypeMapping;
        this.outputFormat = outputFormat;
        this.links = links;
    }

    private void writeSRS() throws IOException {
        json.writeFieldName("srs");
        json.writeStartObject();
        json.writeStringField("type", "name");
        json.writeFieldName("properties");
        json.writeStartObject();
        json.writeStringField("type", outSRS.getAsUrn());
        json.writeEndObject();
        json.writeEndObject();
    }

    protected void writeGeometryHeader(GEO_JSON_GEOMETRY_TYPE type) throws IOException {
        json.writeStringField("type", type.toString());
        json.writeFieldName("coordinates");
    }

    @Override
    protected void writePointGeometry(String x, String y) throws IOException {
        json.writeStringField("type", "Point");
        json.writeFieldName("coordinates");
        json.writeStartArray();
        json.writeRaw(x);
        json.writeRaw(" ,");
        json.writeRaw(y);
        json.writeEndArray();
    }

    @Override
    protected void writeFeatureEnd() throws IOException {
        json.writeEndObject();
        json.writeEndObject();
    }

    @Override
    protected void writeField(TargetMapping mapping, String value, int occurrence) {
        if (value == null || value.isEmpty()) {
            return;
        }

        // handle multiplicity
        String fieldName = mapping.getName();
        if (occurrence > 1) {
            fieldName += "." + occurrence;
        }

        try {
            switch (((GeoJsonPropertyMapping)mapping).getType()) {
                case STRING:
                    json.writeStringField(fieldName, value);
                    break;
                case ID:
                    json.writeStringField(fieldName, value);
                    writeStartProperties();
                    break;
                case NUMBER:
                    // TODO: howto recognize int or double
                    try {
                        json.writeNumberField(fieldName, Long.parseLong(value));
                    } catch (NumberFormatException ex) {
                        json.writeNumberField(fieldName, Double.parseDouble(value));
                        //jsonOut.writeNumberField(mapping.getName(), Long.parseLong(value.split(".")[0]));
                    }
                    break;
            }
        } catch (NumberFormatException ex) {
            //LOGGER.error(FrameworkMessages.NUMBERFORMATEXCEPTION_WRITEMAPPEDFIELD, type.toString(), value);
            analyzeFailed(ex);
        } catch (IOException ex) {
            //LOGGER.error(FrameworkMessages.ERROR_WRITING_RESPONSE_WRITEMAPPEDFIELD, ex.getMessage(), jsonOut.getOutputContext().getCurrentName(), value);
            analyzeFailed(ex);
        }
    }

    // TODO: factor out generic parts, use constant string values/lists
    @Override
    protected void writeGeometry(TargetMapping mapping, SMInputCursor feature) {
        GEO_JSON_GEOMETRY_TYPE type = ((GeoJsonGeometryMapping)mapping).getGeometryType();
        GML_GEOMETRY_TYPE gmlType = GML_GEOMETRY_TYPE.NONE;

        try {
            stopBuffering();

            json.writeObjectFieldStart("geometry");

            Boolean multiContext = false;
            int srsDimension = 2;

            CoordinatesWriterType.Builder cwBuilder = CoordinatesWriterType.builder();
            cwBuilder.format(new JsonCoordinateFormatter(json));

            if (crsTransformer != null) {
                cwBuilder.transformer(crsTransformer);
            }

            Writer coordinatesWriter = null;
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
                        if (type == GEO_JSON_GEOMETRY_TYPE.GENERIC) {
                            type = GEO_JSON_GEOMETRY_TYPE.forGmlType(gmlType);
                        }
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
                        if (type == GEO_JSON_GEOMETRY_TYPE.POLYGON || type == GEO_JSON_GEOMETRY_TYPE.MULTI_POLYGON || type == GEO_JSON_GEOMETRY_TYPE.MULTI_LINE_STRING) {
                            json.writeStartArray();
                        }
                        if (type == GEO_JSON_GEOMETRY_TYPE.MULTI_POLYGON) {
                            json.writeStartArray();
                        }
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

                        //if (coordinatesWriter == null) {
                            coordinatesWriter = cwBuilder.build();
                        //}
                        
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

            if (multiContext) {
                // TODO
                if (type == GEO_JSON_GEOMETRY_TYPE.POLYGON || type == GEO_JSON_GEOMETRY_TYPE.MULTI_POLYGON || type == GEO_JSON_GEOMETRY_TYPE.MULTI_LINE_STRING) {
                    json.writeEndArray();
                }
                if (type == GEO_JSON_GEOMETRY_TYPE.MULTI_POLYGON) {
                    json.writeEndArray();
                }
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

    @Override
    protected void writeEnd() throws IOException {
        if (isFeatureCollection) {
            json.writeEndArray();
            json.writeEndObject();
        }
        json.close();
    }

    @Override
    protected void writeStart(SMInputCursor rootFuture) throws IOException {
        if (isFeatureCollection) {
            json.writeStartObject();
            json.writeStringField("type", "FeatureCollection");

            //this.writeSRS();

            this.writeLinks();

            json.writeFieldName("features");
            json.writeStartArray();
        }
    }

    private void writeLinks() throws IOException {
        json.writeFieldName("links");
        json.writeStartArray();

        for (Wfs3Link link : links) {
            json.writeStartObject();
            json.writeStringField("href", link.href);
            json.writeStringField("rel", link.rel);
            json.writeStringField("type", link.type);
            json.writeStringField("title", link.title);
            json.writeEndObject();
        }

        json.writeEndArray();
    }
    
    @Override
    protected void writeStart(Future<SMInputCursor> rootFuture) throws IOException {
        try {
            this.writeStart(rootFuture.get());
        } catch (InterruptedException ex) {
            Logger.getLogger(GeoJsonFeatureWriter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(GeoJsonFeatureWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    


    @Override
    protected void writeFeatureStart(TargetMapping mapping) throws IOException {
        json.writeStartObject();
        json.writeStringField("type", "Feature");

        if (!isFeatureCollection) {
            //this.writeSRS();
            this.writeLinks();
        }



        //jsonOut.writeObjectFieldStart("properties");
    }

    @Override
    protected void writeStartProperties() throws IOException {
        // we buffer the attributes until the geometry is written
        // TODO: only if geometry exists
        startBuffering();

        json.writeObjectFieldStart("properties");
    }


}
