/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.html;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.codelists.CodelistStore;
import de.ii.ldproxy.output.html.MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE;
import de.ii.ldproxy.output.html.MicrodataMapping.MICRODATA_TYPE;
import de.ii.ldproxy.service.SparqlAdapter;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;
import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.dropwizard.views.FallbackMustacheViewRenderer;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.query.api.WfsProxyFeatureTypeMapping;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLAnalyzer;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
import io.dropwizard.views.ViewRenderer;
import org.apache.http.client.utils.URIBuilder;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author zahnen
 */
public class MicrodataFeatureWriter implements GMLAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicrodataFeatureWriter.class);

    private OutputStreamWriter outputStreamWriter;
    protected XMLPathTracker currentPath;
    protected FeatureDTO currentFeature;
    protected String outputFormat; // as constant somewhere
    protected WfsProxyFeatureTypeMapping featureTypeMapping; // reduceToOutputFormat
    protected boolean isFeatureCollection;
    protected boolean isAddress;
    protected List<String> groupings;
    protected boolean isGrouped;
    //protected String query;
    protected MustacheFactory mustacheFactory;
    protected ViewRenderer mustacheRenderer;
    protected int page;
    protected int pageSize;
    protected CrsTransformer crsTransformer;
    protected SparqlAdapter sparqlAdapter;
    protected CodelistStore codelistStore;

    //public String title;
    //public List<FeatureDTO> features;
    //public List<NavigationDTO> breadCrumbs;
    //public List<NavigationDTO> pagination;
    //public List<NavigationDTO> formats;
    public FeatureCollectionView dataset;
    //public String requestUrl;

    private String wfsUrl;
    private String wfsByIdUrl;

    public MicrodataFeatureWriter(OutputStreamWriter outputStreamWriter, WfsProxyFeatureTypeMapping featureTypeMapping, String outputFormat, boolean isFeatureCollection, boolean isAddress, List<String> groupings, boolean isGrouped, String query, int[] range, FeatureCollectionView featureTypeDataset, CrsTransformer crsTransformer, SparqlAdapter sparqlAdapter, CodelistStore codelistStore, ViewRenderer mustacheRenderer) {
        this.outputStreamWriter = outputStreamWriter;
        this.currentPath = new XMLPathTracker();
        this.featureTypeMapping = featureTypeMapping;
        this.outputFormat = outputFormat;
        this.isFeatureCollection = isFeatureCollection;
        this.isAddress = isAddress;
        this.groupings = groupings;
        this.isGrouped = isGrouped;
        //this.query = query;
        this.mustacheFactory = new DefaultMustacheFactory() {
            @Override
            public Reader getReader(String resourceName) {
                final InputStream is = getClass().getResourceAsStream(resourceName);
                if (is == null) {
                    throw new MustacheException("Template " + resourceName + " not found");
                }
                return new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));
            }

            @Override
            public void encode(String value, Writer writer) {
                try {
                    writer.write(value);
                } catch (IOException e) {
                    // ignore
                }
            }
        };

        if (range != null && range.length > 3) {
            this.page = range[2];
            this.pageSize = range[3];
        }
        this.crsTransformer = crsTransformer;

        this.dataset = featureTypeDataset;

        /*try {
            URIBuilder urlBuilder = new URIBuilder(dataset.requestUrl);
            urlBuilder.clearParameters();
            this.wfsUrl = urlBuilder.build().toString();
            this.wfsByIdUrl = urlBuilder.addParameter("SERVICE", "WFS").addParameter("VERSION", "2.0.0").addParameter("REQUEST", "GetFeature").addParameter("STOREDQUERY_ID", "urn:ogc:def:query:OGC-WFS::GetFeatureById").addParameter("ID", "").build().toString();
        } catch (URISyntaxException e) {
            //ignore
        }*/

        this.sparqlAdapter = sparqlAdapter;
        this.codelistStore = codelistStore;
        this.mustacheRenderer = mustacheRenderer;
    }

    @Override
    public void analyzeStart(Future<SMInputCursor> future) {


        LOGGER.debug("START");

        if (isFeatureCollection) {
            try {
                SMInputCursor cursor = future.get();

                int numberMatched = -1;
                try {
                    numberMatched = Integer.parseInt(cursor.getAttrValue("numberMatched"));
                } catch (NumberFormatException e) {
                    // ignore
                }
                int numberReturned = Integer.parseInt(cursor.getAttrValue("numberReturned"));
                int pages = Math.max(page, 0);
                if (numberReturned > 0 && numberMatched > -1) {
                    pages = Math.max(pages, numberMatched / pageSize + (numberMatched % pageSize > 0 ? 1 : 0));
                }

                LOGGER.debug("numberMatched {}", numberMatched);
                LOGGER.debug("numberReturned {}", numberReturned);
                LOGGER.debug("pageSize {}", pageSize);
                LOGGER.debug("page {}", page);
                LOGGER.debug("pages {}", pages);

                ImmutableList.Builder<NavigationDTO> pagination = new ImmutableList.Builder<>();
                ImmutableList.Builder<NavigationDTO> metaPagination = new ImmutableList.Builder<>();
                if (page > 1) {
                    pagination
                            .add(new NavigationDTO("«", "page=1"))
                            .add(new NavigationDTO("‹", "page=" + String.valueOf(page - 1)));
                    metaPagination
                            .add(new NavigationDTO("prev", "page=" + String.valueOf(page - 1)));
                } else {
                    pagination
                            .add(new NavigationDTO("«"))
                            .add(new NavigationDTO("‹"));
                }

                if (numberMatched > -1) {
                    int from = Math.max(1, page - 2);
                    int to = Math.min(pages, from + 4);
                    if (to == pages) {
                        from = Math.max(1, to - 4);
                    }
                    for (int i = from; i <= to; i++) {
                        if (i == page) {
                            pagination.add(new NavigationDTO(String.valueOf(i), true));
                        } else {
                            pagination.add(new NavigationDTO(String.valueOf(i), "page=" + String.valueOf(i)));
                        }
                    }

                    if (page < pages) {
                        pagination
                                .add(new NavigationDTO("›", "page=" + String.valueOf(page + 1)))
                                .add(new NavigationDTO("»", "page=" + String.valueOf(pages)));
                        metaPagination
                                .add(new NavigationDTO("next", "page=" + String.valueOf(page + 1)));
                    } else {
                        pagination
                                .add(new NavigationDTO("›"))
                                .add(new NavigationDTO("»"));
                    }
                } else {
                    int from = Math.max(1, page - 2);
                    int to = page;
                    for (int i = from; i <= to; i++) {
                        if (i == page) {
                            pagination.add(new NavigationDTO(String.valueOf(i), true));
                        } else {
                            pagination.add(new NavigationDTO(String.valueOf(i), "page=" + String.valueOf(i)));
                        }
                    }
                    if (numberReturned >= pageSize) {
                        pagination
                                .add(new NavigationDTO("›", "page=" + String.valueOf(page + 1)));
                        metaPagination
                                .add(new NavigationDTO("next", "page=" + String.valueOf(page + 1)));
                    } else {
                        pagination
                                .add(new NavigationDTO("›"));
                    }
                }



                this.dataset.pagination = pagination.build();
                this.dataset.metaPagination = metaPagination.build();

            } catch (InterruptedException | ExecutionException | XMLStreamException | NumberFormatException ex) {
                //analyzeFailed(ex);
                LOGGER.error("Pagination not supported by WFS");
            }
        }
    }

    @Override
    public void analyzeEnd() {

        try {
            /*Mustache mustache;
            if (isFeatureCollection) {
                mustache = mustacheFactory.compile("featureCollection.mustache");
            } else {
                mustache = mustacheFactory.compile("featureDetails.mustache");
            }
            mustache.execute(outputStreamWriter, dataset).flush();*/
            ((FallbackMustacheViewRenderer)mustacheRenderer).render(dataset, outputStreamWriter);
            outputStreamWriter.flush();
        } catch (Exception e) {
            analyzeFailed(e);
        } catch (Throwable e) {
            // TODO: analyzeFailed(Throwable)
            LOGGER.error("Error writing HTML: {}", e.getClass());
        }
    }

    @Override
    public void analyzeFeatureStart(String id, String nsuri, String localName) {
        currentPath.clear();

        currentFeature = new FeatureDTO();
        if (!isFeatureCollection) {
            currentFeature.idAsUrl = true;
        }

        if (featureTypeMapping.getMappings().isEmpty()) {
            currentFeature.name = localName;
            currentFeature.itemType = "http://schema.org/Place";
            return;
        }

        for (TargetMapping mapping : featureTypeMapping.findMappings(nsuri + ":" + localName, outputFormat)) {
            if (!mapping.isEnabled()) {
                continue;
            }

            currentFeature.name = mapping.getName();
            currentFeature.itemType = ((MicrodataPropertyMapping) mapping).getItemType();
            currentFeature.itemProp = ((MicrodataPropertyMapping) mapping).getItemProp();
        }
    }

    @Override
    public void analyzeFeatureEnd() {
        /*try {
            if (!isGrouped) {
                outputStreamWriter.write("</ul>");
                outputStreamWriter.write("</div>");
            }
            if (isFeatureCollection || isGrouped) {
                outputStreamWriter.write("</li>");
            }

        } catch (IOException e) {
            analyzeFailed(e);
        }*/
        if (currentFeature.name != null)
            currentFeature.name = currentFeature.name.replaceAll("\\{\\{[^}]*\\}\\}", "");
        if (!isFeatureCollection) {
            this.dataset.title = currentFeature.name;
            this.dataset.breadCrumbs.get(dataset.breadCrumbs.size()-1).label = currentFeature.name;
        }
        dataset.features.add(currentFeature);
        currentFeature = null;
    }

    @Override
    public void analyzeAttribute(String nsuri, String localName, String value) {
        currentPath.track(nsuri, "@" + localName);
        String path = currentPath.toString();
        //LOGGER.debug(" - attribute {}", path);

        // on-the-fly mapping
        if (featureTypeMapping.getMappings().isEmpty() && localName.equals("id") && !currentPath.toFieldName().contains(".")) {

            currentFeature.name = value;

            MicrodataPropertyMapping mapping = new MicrodataPropertyMapping();
            mapping.setName("@id");
            mapping.setType(MICRODATA_TYPE.ID);

            writeField(mapping, value, true);

            return;
        }

        for (TargetMapping mapping : featureTypeMapping.findMappings(path, outputFormat)) {
            if (!mapping.isEnabled()) {
                continue;
            }

            writeField((MicrodataPropertyMapping) mapping, value, true);
        }
    }

    @Override
    public void analyzePropertyStart(String nsuri, String localName, int depth, SMInputCursor feature, boolean nil) {
        currentPath.track(nsuri, localName, depth);

        if (featureTypeMapping.getMappings().isEmpty() && !isFeatureCollection) {
            // TODO: detect geometries
            if (GML_GEOMETRY_TYPE.fromString(localName) != GML_GEOMETRY_TYPE.NONE) {

                writeGeometry(MICRODATA_GEOMETRY_TYPE.forGmlType(GML_GEOMETRY_TYPE.fromString(localName)), feature);
            }
            return;
        }

        String path = currentPath.toString();
        String value = "";

        for (TargetMapping mapping : featureTypeMapping.findMappings(path, outputFormat)) {
            if (!mapping.isEnabled() || (isFeatureCollection && !((MicrodataPropertyMapping) mapping).isShowInCollection())) {
                continue;
            }

            // TODO: I guess fieldCounter is for multiplicity

            // TODO: only if not geometry
            if (((MicrodataPropertyMapping) mapping).getType() != MICRODATA_TYPE.GEOMETRY) {
                if (value.isEmpty()) {
                    try {
                        value = feature.collectDescendantText();
                    } catch (XMLStreamException ex) {
                        //LOGGER.error(FrameworkMessages.ERROR_PARSING_GETFEATURE_REQUEST);
                        analyzeFailed(ex);
                    }
                }

                writeField((MicrodataPropertyMapping) mapping, value, false);
            } else {
                // TODO: write geometry
                writeGeometry(((MicrodataGeometryMapping) mapping).getGeometryType(), feature);
            }
        }
    }

    @Override
    public void analyzePropertyText(String nsuri, String localName, int depth, String text) {
        if (featureTypeMapping.getMappings().isEmpty() && !isGeometry(currentPath)) {
            LOGGER.debug("TEXT {} {}", currentPath.toFieldName(), text);
            MicrodataPropertyMapping mapping = new MicrodataPropertyMapping();
            mapping.setName(currentPath.toFieldNameGml());
            mapping.setType(MICRODATA_TYPE.STRING);

            writeField(mapping, text, false);
        }
    }

    private boolean isGeometry(XMLPathTracker path) {
        for (String elem: path.toFieldName().split("\\.")) {
            if (GML_GEOMETRY_TYPE.fromString(elem) != GML_GEOMETRY_TYPE.NONE) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void analyzePropertyEnd(String nsuri, String localName, int depth) {

    }

    @Override
    public void analyzeFailed(Exception e) {
        LOGGER.error("Error writing HTML");
        LOGGER.debug("Error writing HTML", e);
    }

    @Override
    public boolean analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName) {
        return false;
    }

    protected void writeField(MicrodataPropertyMapping mapping, String value, boolean isId) {

        /*if (value == null || value.isEmpty()) {
            return;
        }*/
        if (value == null) {
            value = "";
        }

        if (isId) {
            currentFeature.id = new FeaturePropertyDTO();
            currentFeature.id.value = value;
            currentFeature.id.itemProp = "url";
            if (!isFeatureCollection) {
                this.dataset.title = value;
            }
            if (currentFeature.name == null || currentFeature.name.isEmpty()) {
                currentFeature.name = value;
            }

            if (!isFeatureCollection) {
                FeaturePropertyDTO property = new FeaturePropertyDTO();
                property.name = "id";
                property.value = value;

                currentFeature.addChild(property);
            }
        } else {
            // TODO: better way to de/serialize

            if (mapping.getItemProp() != null && !mapping.getItemProp().isEmpty()) {
                String[] path = mapping.getItemProp().split("::");

                FeaturePropertyDTO lastProperty = null;

                for (int i = 0; i < path.length; i++) {
                    String itemProp = path[i];
                    String itemType = mapping.getItemType();
                    String prefix = "";

                    if (itemProp.contains("[")) {
                        String[] p = itemProp.split("\\[");
                        itemProp = p[0];
                        String[] props = p[1].split("=");
                        if (props[0].equals("itemType")) {
                            itemType = p[1].split("=")[1];
                            itemType = itemType.substring(0, itemType.indexOf(']'));
                        } else if (props[0].equals("prefix")) {
                            prefix = props[1].substring(0, props[1].length()-1);
                        }
                    }//"itemProp": "address[itemType=http://schema.org/PostalAddress]::streetAddress"

                    FeaturePropertyDTO currentProperty = null;
                    boolean knownProperty = false;

                    if (i == 0) {
                        for (FeaturePropertyDTO p : currentFeature.childList) {
                            if (p != null && p.itemProp != null &&  (p.itemProp.equals(itemProp) || p.name.equals(mapping.getName()))) {
                                currentProperty = p;
                                knownProperty = true;
                                break;
                            }
                        }
                    } else if (lastProperty != null) {
                        for (FeaturePropertyDTO p : lastProperty.childList) {
                            if (p != null &&  (p.itemProp.equals(itemProp) || p.name.equals(mapping.getName()))) {
                                currentProperty = p;
                                knownProperty = true;
                                break;
                            }
                        }
                    }

                    if (currentProperty == null) {
                        currentProperty = new FeaturePropertyDTO();
                        currentProperty.itemProp = itemProp;
                        currentProperty.itemType = itemType;

                        if (i == 0 && !knownProperty) {
                            currentFeature.addChild(currentProperty);
                        }
                    }


                    if (i == path.length - 1) {
                        currentProperty.name = mapping.getName();
                        if (currentProperty.value != null) {
                            currentProperty.value += prefix + value;
                        } else {
                            currentProperty.value = value;
                        }

                        int pos = currentFeature.name.indexOf("{{" + currentProperty.name + "}}");
                        if (pos > -1) {
                            currentFeature.name = currentFeature.name.substring(0, pos) + prefix + value + currentFeature.name.substring(pos);
                        }

                        // TODO
                        if (currentProperty.name.equals("postalCode") && !isFeatureCollection) {
                            Map<String,String> reports = sparqlAdapter.request(currentProperty.value, SparqlAdapter.QUERY.POSTAL_CODE_EXACT);
                            if (!reports.isEmpty()) {
                                currentFeature.links = new FeaturePropertyDTO();
                                currentFeature.links.name = "announcements";
                                for (Map.Entry<String, String> id : reports.entrySet()) {
                                    FeaturePropertyDTO link = new FeaturePropertyDTO();
                                    link.value = id.getKey();
                                    link.name = id.getValue() + " (" + id.getKey().substring(id.getKey().lastIndexOf('/') + 1) + ")";
                                    currentFeature.links.addChild(link);
                                }
                            }
                        }
                    }

                    if (lastProperty != null && !knownProperty) {
                        lastProperty.addChild(currentProperty);
                    }

                    lastProperty = currentProperty;
                }
            } else {
                FeaturePropertyDTO property = new FeaturePropertyDTO();
                property.name = mapping.getName();
                property.value = value;
                property.itemType = mapping.getItemType();
                property.itemProp = mapping.getItemProp();

                if(mapping.getType() == MICRODATA_TYPE.DATE) {
                    try {
                        DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd['T'HH:mm:ss][X]");
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(mapping.getFormat());
                        TemporalAccessor ta = parser.parseBest(value, OffsetDateTime::from, LocalDateTime::from, LocalDate::from);
                        property.value = formatter.format(ta);
                    } catch (Exception e) {
                        //ignore
                    }
                } /*else if(mapping.getType() == MICRODATA_TYPE.STRING && mapping.getFormat() != null && !mapping.getFormat().isEmpty()) {
                    try {
                        property.value = mapping.getFormat().replace("{{value}}", value).replace("{{wfs}}", this.wfsUrl != null ? this.wfsUrl : "").replace("{{wfs-by-id}}", this.wfsByIdUrl != null ? this.wfsByIdUrl : "");
                    } catch (Exception e) {
                        //ignore
                        LOGGER.debug("err", e);
                    }
                }*/
                if (property.value.startsWith("http://") || property.value.startsWith("https://")) {
                    if (property.value.endsWith(".png") || property.value.endsWith(".jpg") || property.value.endsWith(".gif")) {
                        property.isImg = true;
                    } else {
                        property.isUrl = true;
                    }
                }
                if (mapping.getCodelist() != null) {
                    String resolvedValue = null;
                    try {
                        resolvedValue = codelistStore.getResource(mapping.getCodelist()).getEntries().get(property.value);
                    } catch (Exception e) {
                        //ignore
                    }
                    property.value = resolvedValue != null ? resolvedValue : property.value;
                }

                currentFeature.addChild(property);

                int pos = currentFeature.name.indexOf("{{" + property.name + "}}");
                if (pos > -1) {
                    currentFeature.name = currentFeature.name.substring(0, pos) + property.value + currentFeature.name.substring(pos);
                }

                // TODO
                if (property.name.equals("postalCode") && !isFeatureCollection) {
                    Map<String,String> reports = sparqlAdapter.request(property.value, SparqlAdapter.QUERY.POSTAL_CODE_EXACT);
                    if (!reports.isEmpty()) {
                        currentFeature.links = new FeaturePropertyDTO();
                        currentFeature.links.name = "announcements";
                        for (Map.Entry<String, String> id : reports.entrySet()) {
                            FeaturePropertyDTO link = new FeaturePropertyDTO();
                            link.value = id.getKey();
                            link.name = id.getValue() + " (" + id.getKey().substring(id.getKey().lastIndexOf('/') + 1) + ")";
                            currentFeature.links.addChild(link);
                        }
                    }
                }
            }
        }
    }

    private void writeGeometry(MICRODATA_GEOMETRY_TYPE type, SMInputCursor feature) {
        GML_GEOMETRY_TYPE gmlType = GML_GEOMETRY_TYPE.NONE;
        int partCount = 0;

        FeaturePropertyDTO part = null;
        Writer output = new StringWriter();
        Writer coordinatesWriter = new HtmlTransformingCoordinatesWriter(output, 2, crsTransformer);

        try {
            SMInputCursor geo = feature.descendantElementCursor().advance();

            while (geo.readerAccessible()) {
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
                        currentFeature.geo = new FeaturePropertyDTO();
                        currentFeature.geo.itemType = "http://schema.org/GeoShape";
                        currentFeature.geo.itemProp = "geo";
                        currentFeature.geo.name = "geometry";

                        part = new FeaturePropertyDTO();
                        currentFeature.geo.addChild(part);

                        switch (type) {
                            case LINE_STRING:
                                part.itemProp = "line";
                                break;
                            case POLYGON:
                                part.itemProp = "polygon";
                                break;
                        }
                    }

                } else if (geo.getCurrEvent().equals(SMEvent.END_ELEMENT)
                        && (geo.hasLocalName("exterior") || geo.hasLocalName("interior")
                        || geo.hasLocalName("outerBoundaryIs") || geo.hasLocalName("innerBoundaryIs")
                        || geo.hasLocalName("LineString"))) {

                } else if (geo.hasLocalName("posList") || geo.hasLocalName("pos") || geo.hasLocalName("coordinates")) {
                    switch (type) {
                        case POINT:
                            currentFeature.geo = new FeaturePropertyDTO();
                            currentFeature.geo.itemType = "http://schema.org/GeoCoordinates";
                            currentFeature.geo.itemProp = "geo";
                            currentFeature.geo.name = "geometry";

                            String[] coordinates = geo.getElemStringValue().split(" ");
                            CoordinateTuple point = new CoordinateTuple(coordinates[0], coordinates[1]);
                            if (crsTransformer != null) {
                                point = crsTransformer.transform(point);
                            }

                            FeaturePropertyDTO longitude = new FeaturePropertyDTO();
                            longitude.name = "longitude";
                            longitude.itemProp = "longitude";
                            longitude.value = point.getXasString();

                            FeaturePropertyDTO latitude = new FeaturePropertyDTO();
                            latitude.name = "latitude";
                            latitude.itemProp = "latitude";
                            latitude.value = point.getYasString();

                            currentFeature.geo.addChild(latitude);
                            currentFeature.geo.addChild(longitude);

                            break;
                        case LINE_STRING:
                        case POLYGON:
                            if (partCount == 1) {
                                try {
                                    geo.processDescendantText(coordinatesWriter, false);
                                    coordinatesWriter.close();
                                } catch (IOException ex) {
                                    // ignore
                                }
                            }
                            break;
                    }

                }
                geo = geo.advance();
            }
        } catch (XMLStreamException e) {
            analyzeFailed(e);
        }

        if (part != null) {
            part.value = output.toString();
        }
    }
}
