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
import de.ii.ldproxy.output.html.MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE;
import de.ii.ldproxy.output.html.MicrodataMapping.MICRODATA_TYPE;
import de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;
import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeMapping;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLAnalyzer;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMInputCursor;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
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
    protected CrsTransformer crsTransformer;

    public String title;
    public List<FeatureDTO> features;
    public List<NavigationDTO> breadCrumbs;
    public List<NavigationDTO> pagination;
    public List<NavigationDTO> formats;
    public DatasetDTO dataset;
    public String requestUrl;

    public MicrodataFeatureWriter(OutputStreamWriter outputStreamWriter, WfsProxyFeatureTypeMapping featureTypeMapping, String outputFormat, boolean isFeatureCollection, boolean isAddress, List<String> groupings, boolean isGrouped, String query, List<NavigationDTO> breadCrumbs, List<NavigationDTO> formats, int[] range, DatasetDTO featureTypeDataset, CrsTransformer crsTransformer, String requestUrl) {
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
        if (range != null && range.length > 2) {
            this.page = range[2];
        }
        this.crsTransformer = crsTransformer;

        this.features = new ArrayList<>();
        this.breadCrumbs = breadCrumbs;
        this.formats = formats;
        this.dataset = featureTypeDataset;
        if (dataset != null) {
            this.title = dataset.title;
        }
        if (!isFeatureCollection) {
            this.requestUrl = requestUrl;
        }
    }

    @Override
    public void analyzeStart(Future<SMInputCursor> future) {


        LOGGER.getLogger().debug("START");

        try {
            SMInputCursor cursor = future.get();

            int numberMatched = Integer.parseInt(cursor.getAttrValue("numberMatched"));
            int numberReturned = Integer.parseInt(cursor.getAttrValue("numberReturned"));
            int pages = Math.max(page, numberMatched / numberReturned + (numberMatched % numberReturned > 0 ? 1 : 0));

            LOGGER.getLogger().debug("numberMatched {}", numberMatched);
            LOGGER.getLogger().debug("numberReturned {}", numberReturned);
            LOGGER.getLogger().debug("page {}", page);
            LOGGER.getLogger().debug("pages {}", pages);

            ImmutableList.Builder<NavigationDTO> pagination = new ImmutableList.Builder<>();
            if (page > 1) {
                pagination
                        .add(new NavigationDTO("&laquo;", "?page=1"))
                        .add(new NavigationDTO("&lsaquo;", "?page=" + String.valueOf(page - 1)));
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
                    pagination.add(new NavigationDTO(String.valueOf(i), "?page=" + String.valueOf(i)));
                }
            }

            if (page < pages) {
                pagination
                        .add(new NavigationDTO("&rsaquo;", "?page=" + String.valueOf(page + 1)))
                        .add(new NavigationDTO("&raquo;", "?page=" + String.valueOf(pages)));
            } else {
                pagination
                        .add(new NavigationDTO("&rsaquo;"))
                        .add(new NavigationDTO("&raquo;"));
            }

            this.pagination = pagination.build();

        } catch (InterruptedException | ExecutionException | XMLStreamException | NumberFormatException ex) {
            analyzeFailed(ex);
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
            mustache.execute(outputStreamWriter, this).flush();
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
        features.add(currentFeature);
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
        LOGGER.getLogger().error("Error writing HTML", e);
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
                this.title = value;
            }
        } else {
            // TODO: better way to de/serialize

            if (mapping.getItemProp() != null && !mapping.getItemProp().isEmpty()) {
                String[] path = mapping.getItemProp().split("::");

                FeaturePropertyDTO lastProperty = null;

                for (int i = 0; i < path.length; i++) {
                    String itemProp = path[i];
                    String itemType = mapping.getItemType();

                    if (itemProp.contains("[")) {
                        String[] p = itemProp.split("\\[");
                        itemProp = p[0];
                        itemType = p[1].split("=")[1];
                        itemType = itemType.substring(0, itemType.indexOf(']'));
                    }//"itemProp": "address[itemType=http://schema.org/PostalAddress]::streetAddress"

                    FeaturePropertyDTO currentProperty = null;

                    if (i == 0) {
                        for (FeaturePropertyDTO p : currentFeature.childList) {
                            if (p.itemProp.equals(itemProp)) {
                                currentProperty = p;
                                break;
                            }
                        }
                    }
                    if (currentProperty == null) {
                        currentProperty = new FeaturePropertyDTO();
                        currentProperty.itemProp = itemProp;
                        currentProperty.itemType = itemType;

                        if (i == 0) {
                            currentFeature.addChild(currentProperty);
                        }
                    }


                    if (i == path.length - 1) {
                        currentProperty.name = mapping.getName();
                        currentProperty.value = value;
                    }

                    if (lastProperty != null) {
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

                currentFeature.addChild(property);
            }
        }
    }

    private void writeGeometry(MICRODATA_GEOMETRY_TYPE type, SMInputCursor feature) {
        GML_GEOMETRY_TYPE gmlType = GML_GEOMETRY_TYPE.NONE;

        try {
            SMInputCursor geo = feature.descendantElementCursor().advance();

            while (geo.readerAccessible()) {
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
                            break;
                    }

                }
                geo = geo.advance();
            }
        } catch (XMLStreamException e) {
            analyzeFailed(e);
        }
    }
}
