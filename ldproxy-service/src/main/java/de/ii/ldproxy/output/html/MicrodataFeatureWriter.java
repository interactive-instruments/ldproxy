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
package de.ii.ldproxy.output.html;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.gml2json.CoordinatesWriterType;
import de.ii.ldproxy.output.html.MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE;
import de.ii.ldproxy.output.html.MicrodataMapping.MICRODATA_TYPE;
import de.ii.ldproxy.service.SparqlAdapter;
import de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;
import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeMapping;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLAnalyzer;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
import org.apache.http.client.utils.URIBuilder;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMInputCursor;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author zahnen
 */
public class MicrodataFeatureWriter implements GMLAnalyzer {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(MicrodataFeatureWriter.class);

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
    protected int page;
    protected int pageSize;
    protected CrsTransformer crsTransformer;
    protected SparqlAdapter sparqlAdapter;

    //public String title;
    //public List<FeatureDTO> features;
    //public List<NavigationDTO> breadCrumbs;
    //public List<NavigationDTO> pagination;
    //public List<NavigationDTO> formats;
    public FeatureCollectionView dataset;
    //public String requestUrl;

    private String wfsUrl;
    private String wfsByIdUrl;

    public MicrodataFeatureWriter(OutputStreamWriter outputStreamWriter, WfsProxyFeatureTypeMapping featureTypeMapping, String outputFormat, boolean isFeatureCollection, boolean isAddress, List<String> groupings, boolean isGrouped, String query, int[] range, FeatureCollectionView featureTypeDataset, CrsTransformer crsTransformer, SparqlAdapter sparqlAdapter) {
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

        try {
            URIBuilder urlBuilder = new URIBuilder(dataset.requestUrl);
            urlBuilder.clearParameters();
            this.wfsUrl = urlBuilder.build().toString();
            this.wfsByIdUrl = urlBuilder.addParameter("SERVICE", "WFS").addParameter("VERSION", "2.0.0").addParameter("REQUEST", "GetFeature").addParameter("STOREDQUERY_ID", "urn:ogc:def:query:OGC-WFS::GetFeatureById").addParameter("ID", "").build().toString();
        } catch (URISyntaxException e) {
            //ignore
        }

        this.sparqlAdapter = sparqlAdapter;
    }

    @Override
    public void analyzeStart(Future<SMInputCursor> future) {


        LOGGER.getLogger().debug("START");

        if (isFeatureCollection) {
            try {
                SMInputCursor cursor = future.get();

                int numberMatched = Integer.parseInt(cursor.getAttrValue("numberMatched"));
                int numberReturned = Integer.parseInt(cursor.getAttrValue("numberReturned"));
                int pages = Math.max(page, 0);
                if (numberReturned > 0) {
                    pages = Math.max(pages, numberMatched / pageSize + (numberMatched % pageSize > 0 ? 1 : 0));
                }

                LOGGER.getLogger().debug("numberMatched {}", numberMatched);
                LOGGER.getLogger().debug("numberReturned {}", numberReturned);
                LOGGER.getLogger().debug("pageSize {}", pageSize);
                LOGGER.getLogger().debug("page {}", page);
                LOGGER.getLogger().debug("pages {}", pages);

                ImmutableList.Builder<NavigationDTO> pagination = new ImmutableList.Builder<>();
                ImmutableList.Builder<NavigationDTO> metaPagination = new ImmutableList.Builder<>();
                if (page > 1) {
                    pagination
                            .add(new NavigationDTO("&laquo;", "page=1"))
                            .add(new NavigationDTO("&lsaquo;", "page=" + String.valueOf(page - 1)));
                    metaPagination
                            .add(new NavigationDTO("prev", "page=" + String.valueOf(page - 1)));
                } else {
                    pagination
                            .add(new NavigationDTO("&laquo;"))
                            .add(new NavigationDTO("&lsaquo;"));
                }

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
                            .add(new NavigationDTO("&rsaquo;", "page=" + String.valueOf(page + 1)))
                            .add(new NavigationDTO("&raquo;", "page=" + String.valueOf(pages)));
                    metaPagination
                            .add(new NavigationDTO("next", "page=" + String.valueOf(page + 1)));
                } else {
                    pagination
                            .add(new NavigationDTO("&rsaquo;"))
                            .add(new NavigationDTO("&raquo;"));
                }

                this.dataset.pagination = pagination.build();
                this.dataset.metaPagination = metaPagination.build();

            } catch (InterruptedException | ExecutionException | XMLStreamException | NumberFormatException ex) {
                //analyzeFailed(ex);
                LOGGER.getLogger().error("Pagination not supported by WFS");
            }
        }
    }

    @Override
    public void analyzeEnd() {

        try {
            Mustache mustache;
            if (isFeatureCollection) {
                mustache = mustacheFactory.compile("featureCollection.mustache");
            } else {
                mustache = mustacheFactory.compile("featureDetails.mustache");
            }
            mustache.execute(outputStreamWriter, dataset).flush();
        } catch (Exception e) {
            analyzeFailed(e);
        } catch (Throwable e) {
            // TODO: analyzeFailed(Throwable)
            LOGGER.getLogger().error("Error writing HTML: {}", e.getClass());
        }
    }

    @Override
    public void analyzeFeatureStart(String id, String nsuri, String localName) {
        currentPath.clear();

        currentFeature = new FeatureDTO();
        if (!isFeatureCollection) {
            currentFeature.idAsUrl = true;
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
    public void analyzePropertyEnd(String s, String s1, int i) {

    }

    @Override
    public void analyzeFailed(Exception e) {
        LOGGER.getLogger().error("Error writing HTML");
        LOGGER.getLogger().debug("Error writing HTML", e);
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
                        DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd['T'HH:mm:ss[X]]");
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(mapping.getFormat());
                        TemporalAccessor ta = parser.parseBest(value, OffsetDateTime::from, LocalDateTime::from, LocalDate::from);
                        property.value = formatter.format(ta);
                    } catch (Exception e) {
                        //ignore
                    }
                } else if(mapping.getType() == MICRODATA_TYPE.STRING && mapping.getFormat() != null && !mapping.getFormat().isEmpty()) {
                    try {
                        property.value = mapping.getFormat().replace("{{value}}", value).replace("{{wfs}}", this.wfsUrl != null ? this.wfsUrl : "").replace("{{wfs-by-id}}", this.wfsByIdUrl != null ? this.wfsByIdUrl : "");
                    } catch (Exception e) {
                        //ignore
                        LOGGER.getLogger().debug("err", e);
                    }
                }
                if (property.value.startsWith("http://") || property.value.startsWith("https://")) {
                    if (property.value.endsWith(".png") || property.value.endsWith(".jpg") || property.value.endsWith(".gif")) {
                        property.isImg = true;
                    } else {
                        property.isUrl = true;
                    }
                }

                currentFeature.addChild(property);

                int pos = currentFeature.name.indexOf("{{" + property.name + "}}");
                if (pos > -1) {
                    currentFeature.name = currentFeature.name.substring(0, pos) + value + currentFeature.name.substring(pos);
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
