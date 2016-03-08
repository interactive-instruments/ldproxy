package de.ii.ldproxy.output.jsonld;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import de.ii.ldproxy.gml2json.AbstractFeatureWriter;
import de.ii.ldproxy.output.html.DatasetView;
import de.ii.ldproxy.output.html.HtmlTransformingCoordinatesWriter;
import de.ii.ldproxy.output.html.MicrodataGeometryMapping;
import de.ii.ldproxy.output.html.MicrodataPropertyMapping;
import de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;
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
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static de.ii.ldproxy.output.html.MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE;

/**
 * @author zahnen
 */
public class JsonLdOutputWriter extends AbstractFeatureWriter {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(JsonLdOutputWriter.class);

    private int objectDepth;
    private String lastItemProp;
    private String lastValue;
    private String requestUrl;
    private DatasetView dataset;

    public JsonLdOutputWriter(JsonGenerator jsonOut, ObjectMapper jsonMapper, boolean isFeatureCollection, WfsProxyFeatureTypeMapping featureTypeMapping, String outputFormat, CrsTransformer crsTransformer, URI requestUri, DatasetView dataset) {
        super(jsonOut, jsonMapper, isFeatureCollection, crsTransformer);
        this.featureTypeMapping = featureTypeMapping;
        this.outputFormat = outputFormat;
        this.objectDepth = 0;
        this.requestUrl = requestUri.toString().split("\\?")[0];
        if (!isFeatureCollection) {
            int sl = requestUrl.substring(0, requestUrl.length() - 2).lastIndexOf('/');
            this.requestUrl = requestUrl.substring(0, sl + 1);
        }
        this.dataset = dataset;
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
        //json.writeEndObject();
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
            json.writeStartObject();
            json.writeObjectFieldStart("@context");
            json.writeStringField("@vocab", "https://schema.org/");
            json.writeObjectFieldStart("features");
            json.writeStringField("@container", "@set");
            json.writeEndObject();
            json.writeEndObject();

            json.writeStringField("@type", "Dataset");
            json.writeStringField("@id", requestUrl);
            json.writeStringField("name", dataset.title);
            json.writeStringField("description", dataset.description);
            json.writeStringField("url", requestUrl);
            json.writeObjectFieldStart("isPartOf");
            json.writeStringField("@type", "Dataset");
            json.writeStringField("url", requestUrl.substring(0, requestUrl.substring(0, requestUrl.length() - 2).lastIndexOf('/') + 1));
            json.writeEndObject();
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

            json.writeFieldName("features");
            json.writeStartArray();
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
                json.writeStringField("@context", type.substring(0, sep));
            }
            json.writeStringField("@type", type.substring(sep + 1));
        }

        if (!isFeatureCollection) {
            //this.writeSRS();
        }


        //json.writeObjectFieldStart("properties");
    }

    @Override
    protected void writeStartProperties() throws IOException {
        // we buffer the attributes until the geometry is written
        // TODO: only if geometry exists
        startBuffering();

        //json.writeObjectFieldStart("properties");
    }

    @Override
    protected void writeField(TargetMapping mapping, String value) {
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
                                json.writeEndObject();
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
                switch (((MicrodataPropertyMapping) mapping).getType()) {
                    case STRING:
                        json.writeStringField(mapping.getName(), value);
                        break;
                    case ID:
                        json.writeStringField(mapping.getName(), requestUrl + value);
                        // TODO: generate in mapping
                        //json.writeStringField("url", requestUrl + value);
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
            }
        } catch (NumberFormatException ex) {
            //LOGGER.error(FrameworkMessages.NUMBERFORMATEXCEPTION_WRITEMAPPEDFIELD, type.toString(), value);
            analyzeFailed(ex);
        } catch (IOException ex) {
            //LOGGER.error(FrameworkMessages.ERROR_WRITING_RESPONSE_WRITEMAPPEDFIELD, ex.getMessage(), json.getOutputContext().getCurrentName(), value);
            analyzeFailed(ex);
        }
    }

    // TODO: factor out generic parts, use constant string values/lists
    @Override
    protected void writeGeometry(TargetMapping mapping, SMInputCursor feature) {
        if ((isFeatureCollection && !((MicrodataPropertyMapping) mapping).isShowInCollection())) {
            return;
        }

        MICRODATA_GEOMETRY_TYPE type = ((MicrodataGeometryMapping) mapping).getGeometryType();
        GML_GEOMETRY_TYPE gmlType = GML_GEOMETRY_TYPE.NONE;

        int partCount = 0;

        try {
            stopBuffering();

            json.writeObjectFieldStart(mapping.getName());

            /*int srsDimension = 2;

            CoordinatesWriterType.Builder cwBuilder = CoordinatesWriterType.builder(json);

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
                    if (nodeType.isValid() && type == MICRODATA_GEOMETRY_TYPE.forGmlType(nodeType)) {
                        gmlType = nodeType;
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
}
