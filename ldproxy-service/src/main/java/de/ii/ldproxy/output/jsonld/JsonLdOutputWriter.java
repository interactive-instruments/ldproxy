/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.jsonld;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import de.ii.ldproxy.gml2json.AbstractFeatureWriter;
import de.ii.ldproxy.gml2json.CoordinatesWriterType;
import de.ii.ldproxy.gml2json.WktCoordinatesFormatter;
import de.ii.ldproxy.output.generic.AbstractGenericMapping;
import de.ii.ldproxy.output.html.FeatureCollectionView;
import de.ii.ldproxy.output.html.FeaturePropertyDTO;
import de.ii.ldproxy.output.html.HtmlTransformingCoordinatesWriter;
import de.ii.ldproxy.output.html.MicrodataGeometryMapping;
import de.ii.ldproxy.output.html.MicrodataPropertyMapping;
import de.ii.ldproxy.output.jsonld.WktGeometryMapping.WKT_GEOMETRY_TYPE;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;
import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.query.api.WfsProxyFeatureTypeMapping;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static de.ii.ldproxy.output.html.MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE;

/**
 * @author zahnen
 */
public class JsonLdOutputWriter extends AbstractFeatureWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonLdOutputWriter.class);

    private int objectDepth;
    private String lastItemProp;
    private String lastValue;
    private String requestUrl;
    private FeatureCollectionView dataset;
    private String vocab;

    public JsonLdOutputWriter(JsonGenerator jsonOut, ObjectMapper jsonMapper, boolean isFeatureCollection, WfsProxyFeatureTypeMapping featureTypeMapping, String outputFormat, CrsTransformer crsTransformer, URI requestUri, FeatureCollectionView dataset, Map<String, String> rewrites, String vocab) {
        super(jsonOut, jsonMapper, isFeatureCollection, crsTransformer, new JsonLdOnTheFlyMapping());
        this.featureTypeMapping = featureTypeMapping;
        this.outputFormat = outputFormat;
        this.objectDepth = 0;
        // TODO
        this.requestUrl = requestUri.toString().split("\\?")[0];
        for (Map.Entry<String, String> rewrite : rewrites.entrySet()) {
            this.requestUrl = requestUrl.replaceFirst(rewrite.getKey(), rewrite.getValue());
        }
        if (!isFeatureCollection) {
            int sl = requestUrl.substring(0, requestUrl.length() - 2).lastIndexOf('/');
            this.requestUrl = requestUrl.substring(0, sl + 1);
        }
        this.dataset = dataset;
        this.vocab = vocab;
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

    @Override
    protected void writePointGeometry(String x, String y) throws IOException {
        json.writeStringField("longitude", x);
        json.writeStringField("latitude", y);
    }

    @Override
    protected void writeFeatureEnd() throws IOException {
        if (lastValue != null) {
            json.writeString(lastValue);
            this.lastValue = null;
        }
        for (int i = 0; i < objectDepth; i++) {
            json.writeEndObject();
        }
        json.writeEndObject();
        //jsonOut.writeEndObject();
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
        // TODO: name, description, uri, etc.
        if (isFeatureCollection) {
            if (vocab == null) {
                json.writeStartObject();
                json.writeObjectFieldStart("@context");
                json.writeStringField("@vocab", "http://schema.org/");
                //jsonOut.writeObjectFieldStart("features");
                //jsonOut.writeStringField("@container", "@set");
                //jsonOut.writeEndObject();
                for (List<TargetMapping> mappings : featureTypeMapping.findMappings(outputFormat).values()) {
                    for (TargetMapping mapping : mappings) {
                        if (((MicrodataPropertyMapping) mapping).getItemProp() == null && mapping.isEnabled() && mapping.getName() != null && !mapping.getName().startsWith("@") && ((MicrodataPropertyMapping) mapping).isShowInCollection() && !((AbstractGenericMapping)mapping).isSpatial()) {
                            json.writeNullField(mapping.getName());
                        }
                    }
                }
                json.writeEndObject();

                String name = dataset.title;
                if (dataset.indexValue != null) {
                    name += " in " + dataset.indexValue;
                }
                String url = requestUrl;
                if (dataset.indexValue != null) {
                    url += "?" + dataset.index + "=" + dataset.indexValue;
                }
                String parentUrl = requestUrl;
                if (dataset.indexValue == null) {
                    parentUrl = requestUrl.substring(0, requestUrl.substring(0, requestUrl.length() - 2).lastIndexOf('/') + 1);
                }


                json.writeStringField("@type", "Dataset");
                json.writeStringField("@id", url);
                json.writeStringField("name", name);
                json.writeStringField("description", dataset.description);
                json.writeStringField("url", url);
                json.writeObjectFieldStart("isPartOf");
                json.writeStringField("@type", "Dataset");
                json.writeStringField("url", parentUrl);
                json.writeEndObject();
                if (!dataset.hideMetadata) {
                    json.writeStringField("keywords", Joiner.on(',').join(dataset.keywords));
                    json.writeObjectFieldStart("spatial");
                    json.writeStringField("@type", "Place");
                    json.writeObjectFieldStart("geo");
                    json.writeStringField("@type", "GeoShape");
                    json.writeStringField("box", dataset.bbox);
                    json.writeEndObject();
                    json.writeEndObject();
                    if (dataset.metadataUrl != null) {
                        json.writeObjectFieldStart("isPartOf");
                        json.writeStringField("@type", "DataCatalog");
                        json.writeStringField("url", dataset.metadataUrl);
                        json.writeEndObject();
                    }
                }

                // TODO: indices

                // TODO: links
                if (dataset.indexValue != null && dataset.links != null) {
                    json.writeArrayFieldStart("citation");
                    for (FeaturePropertyDTO link : dataset.links.childList) {
                        json.writeStartObject();
                        json.writeStringField("@type", "Report");
                        json.writeStringField("url", link.value);
                        json.writeEndObject();
                    }
                    json.writeEndArray();
                }

                // ???
                json.writeFieldName("mentions");
                json.writeStartArray();
            } else {
                json.writeStartObject();
                json.writeObjectFieldStart("@context");
                json.writeStringField("@vocab", vocab);
                json.writeStringField("geo", "http://www.opengis.net/ont/geosparql#");
                json.writeStringField("geometrie2d", "http://www.opengis.net/ont/geosparql#defaultGeometry");
                json.writeObjectFieldStart("features");
                json.writeStringField("@id", "todo:features");
                json.writeStringField("@container", "@set");
                json.writeEndObject();
                json.writeEndObject();

                json.writeFieldName("features");
                json.writeStartArray();
            }
        }

    }

    @Override
    protected void writeStart(Future<SMInputCursor> rootFuture) throws IOException {
        try {
            this.writeStart(rootFuture.get());
        } catch (InterruptedException ex) {

        } catch (ExecutionException ex) {
        }
    }


    @Override
    protected void writeFeatureStart(TargetMapping mapping) throws IOException {
        json.writeStartObject();
        if (mapping != null) {
            String type = ((MicrodataPropertyMapping) mapping).getItemType();
            int sep = type.lastIndexOf('/');
            if (!isFeatureCollection) {
                //jsonOut.writeStringField("@context", type.substring(0, sep));

                json.writeObjectFieldStart("@context");
                json.writeStringField("@vocab", type.substring(0, sep));
                if (type.contains("demo.bp4mc2.org")) {
                    json.writeStringField("geo", "http://www.opengis.net/ont/geosparql#");
                    json.writeStringField("geometrie2d", "http://www.opengis.net/ont/geosparql#defaultGeometry");
                }
                //jsonOut.writeObjectFieldStart("features");
                //jsonOut.writeStringField("@container", "@set");
                //jsonOut.writeEndObject();
                if (!type.contains("demo.bp4mc2.org")) {
                    for (List<TargetMapping> mappings : featureTypeMapping.findMappings(outputFormat).values()) {
                        for (TargetMapping m : mappings) {
                            if (((MicrodataPropertyMapping) m).getItemProp() == null && m.isEnabled() && m.getName() != null && !m.getName().startsWith("@") && !((AbstractGenericMapping)m).isSpatial()) {
                                json.writeNullField(m.getName());
                            }
                        }
                    }
                }
                json.writeEndObject();
            }
            json.writeStringField("@type", type.substring(sep + 1));
        }

        if (!isFeatureCollection) {
            //this.writeSRS();
        }


        //jsonOut.writeObjectFieldStart("properties");
    }

    @Override
    protected void writeStartProperties() throws IOException {
        // we buffer the attributes until the geometry is written
        // TODO: only if geometry exists
        startBuffering();

        //jsonOut.writeObjectFieldStart("properties");
    }

    @Override
    protected void writeField(TargetMapping mapping, String value, int occurrence) {
        if (value == null || value.isEmpty() || (isFeatureCollection && !((MicrodataPropertyMapping) mapping).isShowInCollection())) {
            return;
        }

        MicrodataPropertyMapping mdMapping = (MicrodataPropertyMapping) mapping;

        try {
            if (mdMapping.getItemProp() != null && !mdMapping.getItemProp().isEmpty()) {
                String[] path = mdMapping.getItemProp().split("::");

                for (int i = 0; i < path.length; i++) {
                    String itemProp = path[i];
                    String itemType = mdMapping.getItemType();
                    String prefix = "";

                    if (itemProp.contains("[")) {
                        String[] p = itemProp.split("\\[");
                        itemProp = p[0];
                        String[] props = p[1].split("=");
                        if (props[0].equals("itemType")) {
                            itemType = p[1].split("=")[1];
                            itemType = itemType.substring(0, itemType.indexOf(']'));
                        } else if (props[0].equals("prefix")) {
                            prefix = props[1].substring(0, props[1].length() - 1);
                        }
                    }//"itemProp": "address[itemType=http://schema.org/PostalAddress]::streetAddress"

                    // TODO
                    if (itemProp != null && !itemProp.isEmpty() && objectDepth == 0) {
                        /*if (i == 0) {
                            for (int i = 0; i < objectDepth; i++) {
                                jsonOut.writeEndObject();
                            }
                            this.objectDepth = 0;
                        }*/
                        json.writeObjectFieldStart(itemProp);
                        objectDepth++;
                        if (itemType != null && !itemType.isEmpty()) {
                            json.writeStringField("@type", itemType);
                        }
                    }
                    if (i == path.length - 1) {
                        if (!itemProp.equals(lastItemProp)) {
                            if (lastValue != null) {
                                json.writeString(lastValue);
                                this.lastValue = null;
                            }
                            json.writeFieldName(itemProp);
                            this.lastItemProp = itemProp;
                        }
                        if (lastValue == null) {
                            this.lastValue = value;
                        } else {
                            this.lastValue += prefix + value;
                        }
                    }
                }
            } else {
                if (lastValue != null) {
                    json.writeString(lastValue);
                    this.lastValue = null;
                }
                for (int i = 0; i < objectDepth; i++) {
                    json.writeEndObject();
                }
                objectDepth = 0;

                switch (((MicrodataPropertyMapping) mapping).getType()) {
                    case STRING:
                        json.writeStringField(mapping.getName(), value);
                        break;
                    case ID:
                        json.writeStringField(mapping.getName(), requestUrl + value);
                        // TODO: generate in mapping
                        if (!isFeatureCollection && vocab == null) {
                            json.writeStringField("url", requestUrl + value);
                        }
                        writeStartProperties();
                        break;
                    case NUMBER:
                        // TODO: howto recognize int or double
                        try {
                            json.writeNumberField(mapping.getName(), Long.parseLong(value));
                        } catch (NumberFormatException ex) {
                            json.writeNumberField(mapping.getName(), Double.parseDouble(value));
                            //jsonOut.writeNumberField(mapping.getName(), Long.parseLong(value.split(".")[0]));
                        }
                        break;
                    case BOOLEAN:
                        json.writeBooleanField(mapping.getName(), value.equals("true"));
                        break;

                }
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
        if ((isFeatureCollection && !((MicrodataPropertyMapping) mapping).isShowInCollection())) {
            return;
        }
        if (vocab != null) {
            writeWktGeometry(mapping, feature);
            return;
        }

        MICRODATA_GEOMETRY_TYPE type = ((MicrodataGeometryMapping) mapping).getGeometryType();
        GML_GEOMETRY_TYPE gmlType = GML_GEOMETRY_TYPE.NONE;

        int partCount = 0;

        try {
            stopBuffering();

            json.writeObjectFieldStart(mapping.getName());

            /*int srsDimension = 2;

            CoordinatesWriterType.Builder cwBuilder = CoordinatesWriterType.builder(jsonOut);

            if (crsTransformer != null) {
                cwBuilder.transformer(crsTransformer);
            }

            Writer coordinatesWriter = cwBuilder.build();
*/
            Writer output = new StringWriter();
            Writer coordinatesWriter = new HtmlTransformingCoordinatesWriter(output, 2, crsTransformer);

            // TODO: MultiPolygon needs one more bracket
            SMInputCursor geo = feature.descendantElementCursor().advance();
            while (geo.readerAccessible()) {
                // TODO: does not work, GML_GEOMETRY_TYPE can only parse schema, not instance
                if (!gmlType.isValid()) {
                    GML_GEOMETRY_TYPE nodeType = GML_GEOMETRY_TYPE.fromString(geo.getLocalName());
                    if (nodeType.isValid()) {
                        if (type == MICRODATA_GEOMETRY_TYPE.GENERIC) {
                            type = MICRODATA_GEOMETRY_TYPE.forGmlType(nodeType);
                        }
                        if (type == MICRODATA_GEOMETRY_TYPE.forGmlType(nodeType)) {
                            gmlType = nodeType;
                        }
                    }
                }

                if (geo.getCurrEvent().equals(SMEvent.START_ELEMENT)
                        && (geo.hasLocalName("exterior") || geo.hasLocalName("interior")
                        || geo.hasLocalName("outerBoundaryIs") || geo.hasLocalName("innerBoundaryIs")
                        || geo.hasLocalName("LineString"))) {

                    partCount++;
                    if (partCount == 1) {
                        json.writeStringField("@type", "GeoShape");
                        switch (type) {
                            case LINE_STRING:
                                json.writeFieldName("line");
                                break;
                            case POLYGON:
                                json.writeFieldName("polygon");
                                break;
                        }
                    }

                } else if (geo.getCurrEvent().equals(SMEvent.END_ELEMENT)
                        && (geo.hasLocalName("exterior") || geo.hasLocalName("interior")
                        || geo.hasLocalName("outerBoundaryIs") || geo.hasLocalName("innerBoundaryIs")
                        || geo.hasLocalName("LineString"))) {
                    if (partCount == 1) {

                    }
                    partCount--;

                } else if (geo.hasLocalName("posList") || geo.hasLocalName("pos") || geo.hasLocalName("coordinates")) {
                    switch (type) {
                        case POINT:
                            json.writeStringField("@type", "GeoCoordinates");

                            String[] coordinates = geo.getElemStringValue().split(" ");
                            CoordinateTuple point = new CoordinateTuple(coordinates[0], coordinates[1]);

                            if (crsTransformer != null) {
                                point = crsTransformer.transform(point);
                            }

                            this.writePointGeometry(point.getXasString(), point.getYasString());

                            break;
                        case LINE_STRING:
                        case POLYGON:
                            if (partCount == 1) {
                                try {
                                    geo.processDescendantText(coordinatesWriter, false);
                                    coordinatesWriter.close();
                                    json.writeString(output.toString());
                                } catch (IOException ex) {
                                    // ignore
                                }
                            }
                            break;
                    }

                }

                geo = geo.advance();

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

    // TODO: factor out generic parts, use constant string values/lists
    protected void writeWktGeometry(TargetMapping mapping, SMInputCursor feature) {
        // TODO: catch cast exception
        WKT_GEOMETRY_TYPE type;
        try {
            type = ((WktGeometryMapping) mapping).getGeometryType();
        } catch (ClassCastException e) {
            return;
        }
        GML_GEOMETRY_TYPE gmlType = GML_GEOMETRY_TYPE.NONE;

        int partCount = 0;

        try {
            stopBuffering();

            json.writeObjectFieldStart(mapping.getName());

            boolean started = false;
            int srsDimension = 2;

            Writer output = new StringWriter();
            CoordinatesWriterType.Builder cwBuilder = CoordinatesWriterType.builder();
            cwBuilder.format(new WktCoordinatesFormatter(output));

            if (crsTransformer != null) {
                cwBuilder.transformer(crsTransformer);
            }

            Writer coordinatesWriter = null;

            // TODO: MultiPolygon needs one more bracket
            SMInputCursor geo = feature.descendantElementCursor().advance();
            while (geo.readerAccessible()) {
                // TODO: does not work, GML_GEOMETRY_TYPE can only parse schema, not instance
                if (!gmlType.isValid()) {
                    GML_GEOMETRY_TYPE nodeType = GML_GEOMETRY_TYPE.fromString(geo.getLocalName());
                    if (nodeType.isValid()) {
                        gmlType = nodeType;
                        if (type == WKT_GEOMETRY_TYPE.GENERIC) {
                            type = WKT_GEOMETRY_TYPE.forGmlType(gmlType);
                        }
                        if (geo.getAttrValue("srsDimension") != null) {
                            srsDimension = Integer.valueOf(geo.getAttrValue("srsDimension"));
                            cwBuilder.dimension(srsDimension);
                        }
                        // TODO: Point
                        if (!started) {
                            json.writeStringField("@type", "geo:Geometry");
                            json.writeFieldName("geo:asWKT");

                            output.write(type.toString());

                            if (type == WKT_GEOMETRY_TYPE.POLYGON || type == WKT_GEOMETRY_TYPE.MULTIPOINT || type == WKT_GEOMETRY_TYPE.MULTILINESTRING || type == WKT_GEOMETRY_TYPE.MULTIPOLYGON) {
                                output.append('(');
                            }
                            if (type == WKT_GEOMETRY_TYPE.MULTIPOLYGON) {
                                output.append('(');
                            }
                            started = true;
                        }
                    }
                }

                if (geo.getCurrEvent().equals(SMEvent.START_ELEMENT)
                        && (geo.hasLocalName("exterior") || geo.hasLocalName("interior")
                        || geo.hasLocalName("outerBoundaryIs") || geo.hasLocalName("innerBoundaryIs")
                        || geo.hasLocalName("LineString"))) {


                } else if (geo.getCurrEvent().equals(SMEvent.END_ELEMENT)
                        && (geo.hasLocalName("exterior") || geo.hasLocalName("interior")
                        || geo.hasLocalName("outerBoundaryIs") || geo.hasLocalName("innerBoundaryIs")
                        || geo.hasLocalName("LineString"))) {


                } else if (geo.hasLocalName("posList") || geo.hasLocalName("pos") || geo.hasLocalName("coordinates")) {
                    //if (coordinatesWriter == null) {
                        coordinatesWriter = cwBuilder.build();
                    //}
                    switch (type) {
                        case POINT:
                        case LINESTRING:
                        case POLYGON:
                        case MULTIPOINT:
                        case MULTILINESTRING:
                        case MULTIPOLYGON:
                            if (partCount > 0) {
                                output.append(',');
                            }
                            partCount++;
                            try {
                                geo.processDescendantText(coordinatesWriter, false);
                                coordinatesWriter.close();
                            } catch (IOException ex) {
                                // ignore
                            }
                            break;
                    }

                }

                geo = geo.advance();

            }

            if (started) {

                if (type == WKT_GEOMETRY_TYPE.POLYGON || type == WKT_GEOMETRY_TYPE.MULTIPOINT || type == WKT_GEOMETRY_TYPE.MULTILINESTRING || type == WKT_GEOMETRY_TYPE.MULTIPOLYGON) {
                    output.append(')');
                }
                if (type == WKT_GEOMETRY_TYPE.MULTIPOLYGON) {
                    output.append(')');
                }

                json.writeString(output.toString());
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
}
