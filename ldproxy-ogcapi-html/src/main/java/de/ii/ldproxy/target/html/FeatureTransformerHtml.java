/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.codelists.Codelist;
import de.ii.ldproxy.codelists.CodelistData;
import de.ii.ldproxy.codelists.CodelistEntity;
import de.ii.ldproxy.target.html.MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE;
import de.ii.ldproxy.target.html.MicrodataMapping.MICRODATA_TYPE;
import de.ii.ldproxy.wfs3.aroundrelations.AroundRelationResolver;
import de.ii.ldproxy.wfs3.aroundrelations.AroundRelationsConfiguration;
import de.ii.ldproxy.wfs3.aroundrelations.AroundRelationsQuery;
import de.ii.ldproxy.wfs3.aroundrelations.SimpleAroundRelationResolver;
import de.ii.ldproxy.wfs3.templates.StringTemplateFilters;
import de.ii.xtraplatform.akka.http.HttpClient;
import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CoordinatesWriterType;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.dropwizard.views.FallbackMustacheViewRenderer;
import de.ii.xtraplatform.feature.provider.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.OnTheFlyMapping;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
import io.dropwizard.views.ViewRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.ii.xtraplatform.util.functional.LambdaWithException.consumerMayThrow;

/**
 * @author zahnen
 */
public class FeatureTransformerHtml implements FeatureTransformer, FeatureTransformer.OnTheFly {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerHtml.class);

    private OutputStreamWriter outputStreamWriter;
    protected XMLPathTracker currentPath;
    protected FeatureDTO currentFeature;
    //protected String outputFormat; // as constant somewhere
    //protected FeatureTypeMapping featureTypeMapping; // reduceToOutputFormat
    protected boolean isFeatureCollection;
    protected boolean isAddress;
    protected List<String> groupings;
    protected boolean isGrouped;
    //protected String query;
    //protected MustacheFactory mustacheFactory;
    protected ViewRenderer mustacheRenderer;
    protected int page;
    protected int pageSize;
    protected CrsTransformer crsTransformer;
    //protected SparqlAdapter sparqlAdapter;
    protected CodelistEntity[] codelists;

    //public String title;
    //public List<FeatureDTO> features;
    //public List<NavigationDTO> breadCrumbs;
    //public List<NavigationDTO> pagination;
    //public List<NavigationDTO> formats;
    public FeatureCollectionView dataset;
    //public String requestUrl;

    private String serviceUrl;

    MICRODATA_GEOMETRY_TYPE currentGeometryType;
    boolean currentGeometryNested;
    CoordinatesWriterType.Builder cwBuilder;
    TargetMapping currentMapping;
    StringBuilder currentValue = new StringBuilder();
    private Writer coordinatesWriter;
    private Writer coordinatesOutput;
    private FeaturePropertyDTO currentGeometryPart;
    private int currentGeometryParts;
    private String currentFormatter;
    private boolean currentGeometryWritten;

    private AroundRelationsQuery aroundRelationsQuery;

    //TODO inject, multiple implementations
    private final AroundRelationResolver aroundRelationResolver;

    private final FeatureTransformationContextHtml transformationContext;
    private final int offset;

    public FeatureTransformerHtml(FeatureTransformationContextHtml transformationContext, HttpClient httpClient) {
        this.outputStreamWriter = new OutputStreamWriter(transformationContext.getOutputStream());
        this.currentPath = new XMLPathTracker();
        //this.featureTypeMapping = featureTypeMapping;
        //this.outputFormat = outputFormat;
        this.isFeatureCollection = transformationContext.isFeatureCollection();
        this.isAddress = false;//isAddress;
        this.groupings = new ArrayList<>();//groupings;
        this.isGrouped = false;//isGrouped;
        //this.query = query;
        this.page = transformationContext.getPage();
        this.pageSize = transformationContext.getLimit();
        this.offset = transformationContext.getOffset();
        /*this.mustacheFactory = new DefaultMustacheFactory() {
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
        };*/
        this.crsTransformer = transformationContext.getCrsTransformer()
                                                   .orElse(null);

        this.dataset = transformationContext.getFeatureTypeDataset();

        /*try {
            URIBuilder urlBuilder = new URIBuilder(dataset.requestUrl);
            urlBuilder.clearParameters();
            this.wfsUrl = urlBuilder.build().toString();
            this.wfsByIdUrl = urlBuilder.addParameter("SERVICE", "WFS").addParameter("VERSION", "2.0.0").addParameter("REQUEST", "GetFeature").addParameter("STOREDQUERY_ID", "urn:ogc:def:query:OGC-WFS::GetFeatureById").addParameter("ID", "").build().toString();
        } catch (URISyntaxException e) {
            //ignore
        }*/
        this.serviceUrl = transformationContext.getServiceUrl();

        //this.sparqlAdapter = null;//sparqlAdapter;
        this.codelists = transformationContext.getCodelists();
        this.mustacheRenderer = transformationContext.getMustacheRenderer();

        this.aroundRelationResolver = new SimpleAroundRelationResolver(httpClient);
        this.transformationContext = transformationContext;
    }

    @Override
    public String getTargetFormat() {
        return Gml2MicrodataMappingProvider.MIME_TYPE;
    }

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws Exception {

        LOGGER.debug("START");

        if (isFeatureCollection && numberReturned.isPresent()) {
            long returned = numberReturned.getAsLong();
            long matched = numberMatched.orElse(-1);

            long pages = Math.max(page, 0);
            if (returned > 0 && matched > -1) {
                pages = Math.max(pages, matched / pageSize + (matched % pageSize > 0 ? 1 : 0));
            }

            LOGGER.debug("numberMatched {}", matched);
            LOGGER.debug("numberReturned {}", returned);
            LOGGER.debug("pageSize {}", pageSize);
            LOGGER.debug("page {}", page);
            LOGGER.debug("pages {}", pages);

            ImmutableList.Builder<NavigationDTO> pagination = new ImmutableList.Builder<>();
            ImmutableList.Builder<NavigationDTO> metaPagination = new ImmutableList.Builder<>();
            if (page > 1) {
                pagination
                        .add(new NavigationDTO("«", String.format("limit=%d&offset=%d", pageSize, 0)))
                        .add(new NavigationDTO("‹", String.format("limit=%d&offset=%d", pageSize, offset - pageSize)));
                metaPagination
                        .add(new NavigationDTO("prev", String.format("limit=%d&offset=%d", pageSize, offset - pageSize)));
            } else {
                pagination
                        .add(new NavigationDTO("«"))
                        .add(new NavigationDTO("‹"));
            }

            if (matched > -1) {
                long from = Math.max(1, page - 2);
                long to = Math.min(pages, from + 4);
                if (to == pages) {
                    from = Math.max(1, to - 4);
                }
                for (long i = from; i <= to; i++) {
                    if (i == page) {
                        pagination.add(new NavigationDTO(String.valueOf(i), true));
                    } else {
                        pagination.add(new NavigationDTO(String.valueOf(i), String.format("limit=%d&offset=%d", pageSize, (i - 1) * pageSize)));
                    }
                }

                if (page < pages) {
                    pagination
                            .add(new NavigationDTO("›", String.format("limit=%d&offset=%d", pageSize, offset + pageSize)))
                            .add(new NavigationDTO("»", String.format("limit=%d&offset=%d", pageSize, (pages - 1) * pageSize)));
                    metaPagination
                            .add(new NavigationDTO("next", String.format("limit=%d&offset=%d", pageSize, offset + pageSize)));
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
                        pagination.add(new NavigationDTO(String.valueOf(i), String.format("limit=%d&offset=%d", pageSize, (i - 1) * pageSize)));
                    }
                }
                if (returned >= pageSize) {
                    pagination
                            .add(new NavigationDTO("›", String.format("limit=%d&offset=%d", pageSize, offset + pageSize)));
                    metaPagination
                            .add(new NavigationDTO("next", String.format("limit=%d&offset=%d", pageSize, offset + pageSize)));
                } else {
                    pagination
                            .add(new NavigationDTO("›"));
                }
            }

            this.dataset.pagination = pagination.build();
            this.dataset.metaPagination = metaPagination.build();

        } else if (isFeatureCollection) {
            //analyzeFailed(ex);
            LOGGER.error("Pagination not supported by feature provider");
        }

        this.aroundRelationsQuery = new AroundRelationsQuery(transformationContext);
    }

    @Override
    public void onEnd() throws Exception {

            /*Mustache mustache;
            if (isFeatureCollection) {
                mustache = mustacheFactory.compile("featureCollection.mustache");
            } else {
                mustache = mustacheFactory.compile("featureDetails.mustache");
            }
            mustache.execute(outputStreamWriter, dataset).flush();*/
        ((FallbackMustacheViewRenderer) mustacheRenderer).render(dataset, outputStreamWriter);
        outputStreamWriter.flush();
    }

    @Override
    public void onFeatureStart(TargetMapping mapping) throws Exception {
        currentFeature = new FeatureDTO();
        if (!isFeatureCollection) {
            currentFeature.idAsUrl = true;
        }

        currentFeature.name = mapping.getName();
        currentFeature.itemType = ((MicrodataPropertyMapping) mapping).getItemType();
        currentFeature.itemProp = ((MicrodataPropertyMapping) mapping).getItemProp();

        if (isFeatureCollection && !aroundRelationsQuery.getRelations()
                                 .isEmpty()) {
            currentFeature.additionalParams = "&relations=" + aroundRelationsQuery.getRelations()
                                                                  .stream()
                                                                  .map(AroundRelationsConfiguration.Relation::getId)
                                                                  .collect(Collectors.joining(",")) + "&resolve=true";
        }
    }

    @Override
    public void onFeatureEnd() throws Exception {
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
            this.dataset.breadCrumbs.get(dataset.breadCrumbs.size() - 1).label = currentFeature.name;
        }
        dataset.features.add(currentFeature);
        currentFeature = null;


        //TODO
        dataset.additionalFeatures = new ArrayList<>();

        if (!isFeatureCollection && aroundRelationsQuery.isReady()) {

            boolean[] started = {false};
            int[] index = {0};

            aroundRelationsQuery.getQueries()
                                .forEach(consumerMayThrow(query -> {


                                    FeaturePropertyDTO featurePropertyDTO = new FeaturePropertyDTO();
                                    featurePropertyDTO.name = query.getConfiguration()
                                                                   .getLabel();
                                    dataset.additionalFeatures.add(featurePropertyDTO);

                                    if (aroundRelationsQuery.isResolve()) {
                                        String resolved = aroundRelationResolver.resolve(query, "&f=html&bare=true");

                                        if (Objects.nonNull(resolved) && resolved.contains("<h4")) {

                                            String url = dataset.getPath() + dataset.getQueryWithout()
                                                                                    .apply("limit,offset");

                                            //TODO multiple relations
                                            featurePropertyDTO.value = resolved.replaceAll("<a class=\"page-link\" href=\".*(&limit(?:=|(?:&#61;))[0-9]+)(?:.*?((?:&|&amp;)offset(?:=|(?:&#61;))[0-9]+))?.*\">", "<a class=\"page-link\" href=\"" + url.substring(0, url.length() - 1) + "$1$2\">");

                                            if (index[0] > 0) {
                                                String limits = IntStream.range(0, index[0])
                                                                         .mapToObj(i -> "5")
                                                                         .collect(Collectors.joining(",", "", ","));
                                                String offsets = IntStream.range(0, index[0])
                                                                          .mapToObj(i -> "0")
                                                                          .collect(Collectors.joining(",", "", ","));
                                                featurePropertyDTO.value = featurePropertyDTO.value.replaceAll("limit(?:=|(?:&#61;))([0-9]+)", "limit=" + limits + "$1");
                                                featurePropertyDTO.value = featurePropertyDTO.value.replaceAll("offset(?:=|(?:&#61;))([0-9]+)", "offset=" + offsets + "$1");
                                            }
                                        } else {
                                            featurePropertyDTO.value = "Keine";
                                        }
                                    } else {
                                        String url = aroundRelationResolver.getUrl(query, "&f=html");

                                        featurePropertyDTO.value = "<a href=\"" + url + "\" target=\"_blank\">Öffnen</a>";
                                    }

                                    index[0]++;
                                }));
        }
    }

    @Override
    public void onPropertyStart(TargetMapping mapping, List<Integer> multiplicities) throws Exception {
        currentMapping = mapping;
    }

    @Override
    public void onPropertyText(String text) throws Exception {
        currentValue.append(text);
    }

    @Override
    public void onPropertyEnd() throws Exception {
        if (currentValue.length() > 0) {
            writeField((MicrodataPropertyMapping) currentMapping, currentValue.toString());
            currentValue.setLength(0);
        }
    }

    protected void writeField(MicrodataPropertyMapping mapping, String value) {

        /*if (value == null || value.isEmpty()) {
            return;
        }*/
        if (value == null) {
            value = "";
        }

        if (mapping.getType() == MICRODATA_TYPE.ID) {
            currentFeature.id = new FeaturePropertyDTO();
            currentFeature.id.value = value;
            currentFeature.id.itemProp = "url";
            if (!isFeatureCollection) {
                this.dataset.title = value;
            }
            if (currentFeature.name == null || currentFeature.name.isEmpty()) {
                currentFeature.name = value;
            }

            if (!isFeatureCollection || mapping.isShowInCollection()) {
                FeaturePropertyDTO property = new FeaturePropertyDTO();
                property.name = mapping.getName();
                property.value = value;

                currentFeature.addChild(property);
            }
        } else {
            // TODO: better way to de/serialize

            if (mapping.getItemProp() != null && !mapping.getItemProp()
                                                         .isEmpty()) {
                String[] path = mapping.getItemProp()
                                       .split("::");

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
                            prefix = props[1].substring(0, props[1].length() - 1);
                        }
                    }//"itemProp": "address[itemType=http://schema.org/PostalAddress]::streetAddress"

                    FeaturePropertyDTO currentProperty = null;
                    boolean knownProperty = false;

                    if (i == 0) {
                        for (FeaturePropertyDTO p : currentFeature.childList) {
                            if (p != null && p.itemProp != null && (p.itemProp.equals(itemProp) || p.name.equals(mapping.getName()))) {
                                currentProperty = p;
                                knownProperty = true;
                                break;
                            }
                        }
                    } else if (lastProperty != null) {
                        for (FeaturePropertyDTO p : lastProperty.childList) {
                            if (p != null && (p.itemProp.equals(itemProp) || p.name.equals(mapping.getName()))) {
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
                        /*if (currentProperty.name.equals("postalCode") && !isFeatureCollection) {
                            Map<String, String> reports = sparqlAdapter.request(currentProperty.value, SparqlAdapter.QUERY.POSTAL_CODE_EXACT);
                            if (!reports.isEmpty()) {
                                currentFeature.links = new FeaturePropertyDTO();
                                currentFeature.links.name = "announcements";
                                for (Map.Entry<String, String> id : reports.entrySet()) {
                                    FeaturePropertyDTO link = new FeaturePropertyDTO();
                                    link.value = id.getKey();
                                    link.name = id.getValue() + " (" + id.getKey()
                                                                         .substring(id.getKey()
                                                                                      .lastIndexOf('/') + 1) + ")";
                                    currentFeature.links.addChild(link);
                                }
                            }
                        }*/
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

                int pos = currentFeature.name.indexOf("{{" + property.name + "}}");
                if (pos > -1) {
                    currentFeature.name = currentFeature.name.substring(0, pos) + property.value + currentFeature.name.substring(pos);
                }


                if (mapping.getCodelist() != null) {
                    //TODO: read into map in OgcApiFeaturesOutputFormatHtml with @Bind(aggregate=true)
                    //  private void bindHello(Hello h) { m_hellos.add(h); }

                    property.value = Arrays.stream(codelists)
                                           .filter(cl -> cl.getId()
                                                           .equals(mapping.getCodelist()))
                                           .findFirst()
                                           .map(cl -> {
                                               String resolvedValue = cl.getValue(property.value);

                                               if (cl.getData()
                                                     .getSourceType() == CodelistData.IMPORT_TYPE.TEMPLATES) {
                                                   resolvedValue = StringTemplateFilters.applyFilterMarkdown(StringTemplateFilters.applyTemplate(resolvedValue, property.value));
                                                   property.isHtml = true;
                                               }

                                               return resolvedValue;
                                           })
                                           .orElse(property.value);
                }

                if (mapping.getType() == MICRODATA_TYPE.DATE) {
                    try {
                        DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd[['T'][' ']HH:mm:ss][.SSS][X]");
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(mapping.getFormat());
                        TemporalAccessor ta = parser.parseBest(value, OffsetDateTime::from, LocalDateTime::from, LocalDate::from);
                        property.value = formatter.format(ta);
                    } catch (Exception e) {
                        //ignore
                    }
                } else if (mapping.getType() == MICRODATA_TYPE.STRING && mapping.getFormat() != null && !mapping.getFormat()
                                                                                                                .isEmpty()) {
                    boolean more = false;
                    if (currentFormatter == null) {

                        String formattedValue = StringTemplateFilters.applyTemplate(mapping.getFormat(), property.value, isHtml -> property.isHtml = isHtml);

                        property.value = formattedValue
                                .replace("{{serviceUrl}}", serviceUrl);
                        int subst = property.value.indexOf("}}");
                        if (subst > -1) {
                            property.value = property.value.substring(0, property.value.indexOf("{{")) + value + property.value.substring(subst + 2);
                            more = property.value.contains("}}");
                        }
                    } else {
                        int subst = currentFormatter.indexOf("}}");
                        if (subst > -1) {
                            property.value = currentFormatter.substring(0, currentFormatter.indexOf("{{")) + value + currentFormatter.substring(subst + 2);
                            more = property.value.contains("}}");
                        }
                    }
                    if (more) {
                        this.currentFormatter = property.value;
                        return;
                    } else {
                        currentFormatter = null;
                    }
                }
                if (property.value.startsWith("http://") || property.value.startsWith("https://")) {
                    if (property.value.toLowerCase()
                                      .endsWith(".png") || property.value.toLowerCase()
                                                                         .endsWith(".jpg") || property.value.toLowerCase()
                                                                                                            .endsWith(".gif")) {
                        property.isImg = true;
                    } else {
                        property.isUrl = true;
                    }
                }

                currentFeature.addChild(property);

                // TODO
                /*if (property.name.equals("postalCode") && !isFeatureCollection) {
                    Map<String, String> reports = sparqlAdapter.request(property.value, SparqlAdapter.QUERY.POSTAL_CODE_EXACT);
                    if (!reports.isEmpty()) {
                        currentFeature.links = new FeaturePropertyDTO();
                        currentFeature.links.name = "announcements";
                        for (Map.Entry<String, String> id : reports.entrySet()) {
                            FeaturePropertyDTO link = new FeaturePropertyDTO();
                            link.value = id.getKey();
                            link.name = id.getValue() + " (" + id.getKey()
                                                                 .substring(id.getKey()
                                                                              .lastIndexOf('/') + 1) + ")";
                            currentFeature.links.addChild(link);
                        }
                    }
                }*/
            }
        }
    }

    @Override
    public void onGeometryStart(TargetMapping mapping, SimpleFeatureGeometry type, Integer dimension) throws Exception {
        if (Objects.isNull(mapping)) return;

        dataset.hideMap = false;

        final MicrodataGeometryMapping geometryMapping = (MicrodataGeometryMapping) mapping;
        if (isFeatureCollection && !((MicrodataGeometryMapping) mapping).isShowInCollection()) return;

        currentGeometryType = geometryMapping.getGeometryType();
        if (currentGeometryType == MICRODATA_GEOMETRY_TYPE.GENERIC) {
            currentGeometryType = MICRODATA_GEOMETRY_TYPE.forGmlType(type);
        }

        coordinatesOutput = new StringWriter();
        //coordinatesWriter = new HtmlTransformingCoordinatesWriter(coordinatesOutput, Objects.nonNull(dimension) ? dimension : 2, crsTransformer);

        // for around-relations
        this.cwBuilder = CoordinatesWriterType.builder();

        cwBuilder.format(new MicrodataCoordinatesFormatter(coordinatesOutput));

        if (transformationContext.getCrsTransformer()
                                 .isPresent()) {
            cwBuilder.transformer(transformationContext.getCrsTransformer()
                                                       .get());
        }

        if (dimension != null) {
            cwBuilder.dimension(dimension);
        }

        coordinatesWriter = cwBuilder.build();


        currentGeometryParts = 0;
    }

    @Override
    public void onGeometryNestedStart() throws Exception {
        if (currentGeometryType == null) return;

        currentGeometryParts++;
        if (currentGeometryParts == 1) {
            currentFeature.geo = new FeaturePropertyDTO();
            currentFeature.geo.itemType = "http://schema.org/GeoShape";
            currentFeature.geo.itemProp = "geo";
            currentFeature.geo.name = "geometry";

            currentGeometryPart = new FeaturePropertyDTO();
            currentFeature.geo.addChild(currentGeometryPart);

            switch (currentGeometryType) {
                case LINE_STRING:
                    currentGeometryPart.itemProp = "line";
                    break;
                case POLYGON:
                    currentGeometryPart.itemProp = "polygon";
                    break;
            }
        }
    }

    @Override
    public void onGeometryCoordinates(String text) throws Exception {
        if (currentGeometryType == null) return;

        switch (currentGeometryType) {
            case POINT:
                currentFeature.geo = new FeaturePropertyDTO();
                currentFeature.geo.itemType = "http://schema.org/GeoCoordinates";
                currentFeature.geo.itemProp = "geo";
                currentFeature.geo.name = "geometry";

                String[] coordinates = text.split(" ");
                CoordinateTuple point = new CoordinateTuple(coordinates[0], coordinates[1]);
                if (crsTransformer != null) {
                    point = crsTransformer.transform(point, false);
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
                if (!currentGeometryWritten) {
                    try {
                        coordinatesWriter.append(text);
                        coordinatesWriter.close();
                        currentGeometryWritten = true;
                    } catch (IOException ex) {
                        // ignore
                    }
                }
                break;
        }

        if (!isFeatureCollection && aroundRelationsQuery.isActive()) {
            aroundRelationsQuery.addCoordinates(text, cwBuilder);
        }
    }

    @Override
    public void onGeometryNestedEnd() throws Exception {
        if (currentGeometryType == null) return;
    }

    @Override
    public void onGeometryEnd() throws Exception {
        if (currentGeometryType == null) return;

        if (currentGeometryPart != null) {
            currentGeometryPart.value = coordinatesOutput.toString();
        }

        if (!isFeatureCollection && aroundRelationsQuery.isActive()) {
            aroundRelationsQuery.computeBbox(currentGeometryType.toSimpleFeatureGeometry());
        }

        currentGeometryType = null;
        currentGeometryWritten = false;
    }

    @Override
    public OnTheFlyMapping getOnTheFlyMapping() {
        return new OnTheFlyMappingHtml();
    }

}
