/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformations;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.target.html.MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE;
import de.ii.ldproxy.wfs3.aroundrelations.AroundRelationResolver;
import de.ii.ldproxy.wfs3.aroundrelations.AroundRelationsConfiguration;
import de.ii.ldproxy.wfs3.aroundrelations.AroundRelationsQuery;
import de.ii.ldproxy.wfs3.aroundrelations.SimpleAroundRelationResolver;
import de.ii.xtraplatform.akka.http.HttpClient;
import de.ii.xtraplatform.dropwizard.views.FallbackMustacheViewRenderer;
import de.ii.xtraplatform.feature.provider.api.FeatureProperty;
import de.ii.xtraplatform.feature.provider.api.FeatureTransformer2;
import de.ii.xtraplatform.feature.provider.api.FeatureType;
import de.ii.xtraplatform.feature.provider.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.transformer.api.OnTheFly;
import de.ii.xtraplatform.feature.transformer.api.OnTheFlyMapping;
import de.ii.xtraplatform.geometries.domain.CoordinateTuple;
import de.ii.xtraplatform.geometries.domain.CrsTransformer;
import de.ii.xtraplatform.geometries.domain.ImmutableCoordinatesTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.ii.xtraplatform.util.functional.LambdaWithException.consumerMayThrow;

/**
 * @author zahnen
 */
public class FeatureTransformerHtml implements FeatureTransformer2, OnTheFly {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerHtml.class);

    private final OutputStreamWriter outputStreamWriter;
    private final FallbackMustacheViewRenderer mustacheRenderer;
    private final boolean isFeatureCollection;
    private final int page;
    private final int pageSize;
    private final int offset;
    private final CrsTransformer crsTransformer;
    private final FeatureCollectionView dataset;
    private AroundRelationsQuery aroundRelationsQuery;
    private final AroundRelationResolver aroundRelationResolver; //TODO inject, multiple implementations
    private final HtmlConfiguration htmlConfiguration;
    private final Map<String, HtmlPropertyTransformations> transformations;
    private final boolean isMicrodataEnabled;

    private ObjectDTO currentFeature;
    private MICRODATA_GEOMETRY_TYPE currentGeometryType;
    private ImmutableCoordinatesTransformer.Builder coordinatesTransformerBuilder;
    private FeatureProperty currentFeatureProperty;
    private StringBuilder currentValue = new StringBuilder();
    private Writer coordinatesWriter;
    private Writer coordinatesOutput;
    private PropertyDTO currentGeometryPart;
    private int currentGeometryParts;
    private boolean currentGeometryWritten;

    public FeatureTransformerHtml(FeatureTransformationContextHtml transformationContext, HttpClient httpClient) {
        this.outputStreamWriter = new OutputStreamWriter(transformationContext.getOutputStream());
        this.isFeatureCollection = transformationContext.isFeatureCollection();
        this.page = transformationContext.getPage();
        this.pageSize = transformationContext.getLimit();
        this.offset = transformationContext.getOffset();
        this.crsTransformer = transformationContext.getCrsTransformer()
                                                   .orElse(null);
        this.dataset = transformationContext.getFeatureTypeDataset();
        this.mustacheRenderer = (FallbackMustacheViewRenderer) transformationContext.getMustacheRenderer();
        this.aroundRelationsQuery = new AroundRelationsQuery(transformationContext);
        this.aroundRelationResolver = new SimpleAroundRelationResolver(httpClient);
        this.htmlConfiguration = transformationContext.getHtmlConfiguration();

        FeatureTypeConfigurationOgcApi featureTypeConfiguration = transformationContext.getApiData()
                                                                                       .getCollections()
                                                                                       .get(transformationContext.getCollectionId());

        Optional<FeatureTransformations> baseTransformations = featureTypeConfiguration
                .getExtension(OgcApiFeaturesCoreConfiguration.class)
                .map(coreConfiguration -> coreConfiguration);

        this.transformations = featureTypeConfiguration
                .getExtension(HtmlConfiguration.class)
                .map(htmlConfiguration -> htmlConfiguration.getTransformations(baseTransformations, transformationContext.getCodelists(), transformationContext.getServiceUrl(), isFeatureCollection))
                .orElse(ImmutableMap.of());

        this.isMicrodataEnabled = transformationContext.getHtmlConfiguration()
                                                       .getMicrodataEnabled();
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
    }

    @Override
    public void onEnd() throws Exception {
        mustacheRenderer.render(dataset, outputStreamWriter);
        outputStreamWriter.flush();
    }

    @Override
    public void onFeatureStart(FeatureType featureType) throws Exception {
      currentFeature = new ObjectDTO();

        if (isFeatureCollection) {
            currentFeature.inCollection = true;
        }

        Optional<String> itemLabelFormat = htmlConfiguration.getItemLabelFormat();
        if (itemLabelFormat.isPresent()) {
            currentFeature.name = itemLabelFormat.get();
        }

        if (isMicrodataEnabled) {
            currentFeature.itemType = "http://schema.org/Place";
        }

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
        if (currentFeature.name != null) {
            currentFeature.name = currentFeature.name.replaceAll("\\{\\{[^}]*\\}\\}", "");
        } else {
            currentFeature.name = currentFeature.id.getFirstValue();
        }

        if (!isFeatureCollection) {
            this.dataset.title = currentFeature.name;
            this.dataset.breadCrumbs.get(dataset.breadCrumbs.size() - 1).label = currentFeature.name;
        }

        dataset.features.add(currentFeature);
        currentFeature = null;

        //TODO: move to ldproxy-wfs3-around-relations
        dataset.additionalFeatures = new ArrayList<>();

        if (!isFeatureCollection && aroundRelationsQuery.isReady()) {

            boolean[] started = {false};
            int[] index = {0};

            aroundRelationsQuery.getQueries()
                                .forEach(consumerMayThrow(query -> {


                                    PropertyDTO propertyDTO = new PropertyDTO();
                                    propertyDTO.name = query.getConfiguration()
                                                                   .getLabel();
                                    dataset.additionalFeatures.add(propertyDTO);
                                    String value = null;

                                    if (aroundRelationsQuery.isResolve()) {
                                        String resolved = aroundRelationResolver.resolve(query, "&f=html&bare=true");

                                        if (Objects.nonNull(resolved) && resolved.contains("<h4")) {

                                            String url = dataset.getPath() + dataset.getQueryWithout()
                                                                                    .apply("limit,offset");

                                            //TODO multiple relations
                                            value = resolved.replaceAll("<a class=\"page-link\" href=\".*(&limit(?:=|(?:&#61;))[0-9]+)(?:.*?((?:&|&amp;)offset(?:=|(?:&#61;))[0-9]+))?.*\">", "<a class=\"page-link\" href=\"" + url.substring(0, url.length() - 1) + "$1$2\">");

                                            if (index[0] > 0) {
                                                String limits = IntStream.range(0, index[0])
                                                                         .mapToObj(i -> "5")
                                                                         .collect(Collectors.joining(",", "", ","));
                                                String offsets = IntStream.range(0, index[0])
                                                                          .mapToObj(i -> "0")
                                                                          .collect(Collectors.joining(",", "", ","));
                                                value = value.replaceAll("limit(?:=|(?:&#61;))([0-9]+)", "limit=" + limits + "$1");
                                                value = value.replaceAll("offset(?:=|(?:&#61;))([0-9]+)", "offset=" + offsets + "$1");
                                            }

                                        } else {
                                            value = "Keine";
                                        }
                                    } else {
                                        String url = aroundRelationResolver.getUrl(query, "&f=html");

                                        value = "<a href=\"" + url + "\" target=\"_blank\">Öffnen</a>";
                                    }
                                    propertyDTO.addValue(value);

                                    index[0]++;
                                }));
        }
    }

    @Override
    public void onPropertyStart(FeatureProperty featureProperty, List<Integer> multiplicities) throws Exception {
        currentFeatureProperty = featureProperty;
    }

    @Override
    public void onPropertyText(String text) throws Exception {
        currentValue.append(text);
    }

    @Override
    public void onPropertyEnd() throws Exception {
        if (currentValue.length() > 0) {
            writeField(currentFeatureProperty, currentValue.toString());
            currentValue.setLength(0);
        }
    }

    protected void writeField(FeatureProperty featureProperty, String value) {

        if (featureProperty.isId()) {
          currentFeature.id = new PropertyDTO();
          currentFeature.id.addValue(value);
          currentFeature.id.itemProp = "url";
        }

        PropertyDTO property = new PropertyDTO();
        property.name = featureProperty.getName();
        property.addValue(value);

        if (currentFeature.name != null) {
            int pos = currentFeature.name.indexOf("{{" + property.name + "}}");
            if (pos > -1) {
                currentFeature.name = currentFeature.name.substring(0, pos) + value + currentFeature.name.substring(pos);
            }
        }

        String tkey = featureProperty.getName()
                                     .replaceAll("\\[.+?\\]", "[]");
        if (transformations.containsKey(tkey)) {

            Optional<ValueDTO> transformedProperty = property.values.size()>0 ?
                    transformations.get(tkey)
                                   .transform(property.values.get(0), featureProperty) :
                    Optional.empty();

            if (transformedProperty.isPresent()) {
                property.values.set(0,transformedProperty.get());
            }
        }
        currentFeature.addChild(property);
    }

    @Override
    public void onGeometryStart(FeatureProperty featureProperty, SimpleFeatureGeometry type,
                                Integer dimension) throws Exception {
        if (Objects.isNull(featureProperty)) return;

        dataset.hideMap = false;

        if (!isMicrodataEnabled && !aroundRelationsQuery.isActive()) return;

        if (transformations.containsKey(featureProperty.getName())) {

            boolean shouldSkipProperty = !transformations.get(featureProperty.getName()).transform(featureProperty).isPresent();

            if (shouldSkipProperty) {
                return;
            }
        }

        currentGeometryType = MICRODATA_GEOMETRY_TYPE.forGmlType(type);

        coordinatesOutput = new StringWriter();

        this.coordinatesTransformerBuilder = ImmutableCoordinatesTransformer.builder();

        coordinatesTransformerBuilder.coordinatesWriter(ImmutableCoordinatesWriterMicrodata.of(coordinatesOutput, Optional.ofNullable(dimension).orElse(2)));

        if (Objects.nonNull(crsTransformer)) {
            coordinatesTransformerBuilder.crsTransformer(crsTransformer);
        }

        if (dimension != null) {
            coordinatesTransformerBuilder.sourceDimension(dimension);
            coordinatesTransformerBuilder.targetDimension(dimension);
        }

        coordinatesWriter = coordinatesTransformerBuilder.build();


        currentGeometryParts = 0;
    }

    @Override
    public void onGeometryNestedStart() throws Exception {
        if (currentGeometryType == null) return;

        currentGeometryParts++;
        if (currentGeometryParts == 1) {
            currentFeature.geo = new PropertyDTO();
            currentFeature.geo.itemType = "http://schema.org/GeoShape";
            currentFeature.geo.itemProp = "geo";
            currentFeature.geo.name = "geometry";

            currentGeometryPart = new PropertyDTO();
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
                currentFeature.geo = new PropertyDTO();
                currentFeature.geo.itemType = "http://schema.org/GeoCoordinates";
                currentFeature.geo.itemProp = "geo";
                currentFeature.geo.name = "geometry";

                String[] coordinates = text.split(" ");
                CoordinateTuple point = new CoordinateTuple(coordinates[0], coordinates[1]);
                if (crsTransformer != null) {
                    point = crsTransformer.transform(point, false);
                }

                PropertyDTO longitude = new PropertyDTO();
                longitude.name = "longitude";
                longitude.itemProp = "longitude";
                longitude.addValue(point.getXasString());

                PropertyDTO latitude = new PropertyDTO();
                latitude.name = "latitude";
                latitude.itemProp = "latitude";
                latitude.addValue(point.getYasString());

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
            aroundRelationsQuery.addCoordinates(text, coordinatesTransformerBuilder);
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
            currentGeometryPart.addValue(coordinatesOutput.toString());
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
