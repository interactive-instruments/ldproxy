/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.codelists.Codelist;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.Dataset;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.domain.Wfs3Collection;
import de.ii.ldproxy.ogcapi.domain.Wfs3Link;
import de.ii.ldproxy.wfs3.api.FeatureTransformationContext;
import de.ii.ldproxy.wfs3.api.Wfs3LinksGenerator;
import de.ii.ldproxy.wfs3.api.Wfs3OutputFormatExtension;
import de.ii.xtraplatform.akka.http.Http;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;
import org.apache.felix.ipojo.annotations.BindingPolicy;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.MOVED_PERMANENTLY;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3OutputFormatHtml implements ConformanceClass, Wfs3OutputFormatExtension {

    static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .main(MediaType.TEXT_HTML_TYPE)
            //.qs(900)
            .build();

    @Context
    private BundleContext bc;

    @Requires
    private HtmlConfig htmlConfig;

    @Requires
    private Dropwizard dropwizard;

    @Requires(optional = true)
    private Codelist[] codelists;

    @Requires
    private Http http;

    @Requires
    private KeyValueStore keyValueStore;

    @Requires(policy= BindingPolicy.DYNAMIC_PRIORITY)
    private I18n i18n;

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/html";
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForDataset(OgcApiDatasetData datasetData) {
        return isExtensionEnabled(datasetData, HtmlConfiguration.class);
    }


    @Override
    public Response getConformanceResponse(List<ConformanceClass> wfs3ConformanceClasses, String serviceLabel,
                                           OgcApiMediaType ogcApiMediaType, List<OgcApiMediaType> alternativeMediaTypes,
                                           URICustomizer uriCustomizer, String staticUrlPrefix) {

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Services", uriCustomizer.copy()
                                                                .removeLastPathSegments(2)
                                                                .toString()))
                .add(new NavigationDTO(serviceLabel, uriCustomizer.copy()
                                                                  .removeLastPathSegments(1)
                                                                  .toString()))
                .add(new NavigationDTO("Conformance Classes"))
                .build();

        final Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();
        List<Wfs3Link> links = wfs3LinksGenerator.generateAlternateLinks(uriCustomizer.copy(), true, alternativeMediaTypes);

        Wfs3ConformanceClassesView wfs3ConformanceClassesView = new Wfs3ConformanceClassesView(wfs3ConformanceClasses.stream()
                                                                                                                     .map(ConformanceClass::getConformanceClass)
                                                                                                                     .collect(Collectors.toList()), breadCrumbs, links, staticUrlPrefix, htmlConfig);
        return Response.ok()
                       .type(ogcApiMediaType.metadata())
                       .entity(wfs3ConformanceClassesView)
                       .build();
    }

    @Override
    public Response getDatasetResponse(Dataset wfs3Collections, OgcApiDatasetData datasetData,
                                       OgcApiMediaType mediaType,
                                       List<OgcApiMediaType> alternativeMediaTypes, URICustomizer uriCustomizer,
                                       String staticUrlPrefix, boolean isCollections) {
        //TODO: locales from request context
        String datasetsTitle = i18n.get("datasets");

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(datasetsTitle, uriCustomizer.copy()
                                                                   .removeLastPathSegments(uriCustomizer.isLastPathSegment("collections") ? 2 : 1)
                                                                   .toString()))
                .add(new NavigationDTO(datasetData.getLabel()))
                .build();


        Wfs3DatasetView wfs3DatasetView = new Wfs3DatasetView(datasetData, wfs3Collections, breadCrumbs, staticUrlPrefix, htmlConfig);

        return Response.ok()
                       .type(mediaType.metadata())
                       .entity(wfs3DatasetView)
                       .build();
    }

    @Override
    public Response getCollectionResponse(Wfs3Collection wfs3Collection, OgcApiDatasetData datasetData,
                                          OgcApiMediaType mediaType, List<OgcApiMediaType> alternativeMediaTypes,
                                          URICustomizer uriCustomizer, String collectionName) {
        return Response.status(MOVED_PERMANENTLY)
                       .header(HttpHeaders.LOCATION, uriCustomizer.copy()
                                                                  .ensureLastPathSegment("items")
                                                                  .toString())
                       .build();
    }

    @Override
    public boolean canTransformFeatures() {
        return true;
    }

    @Override
    public Optional<FeatureTransformer> getFeatureTransformer(FeatureTransformationContext transformationContext) {
        OgcApiDatasetData serviceData = transformationContext.getServiceData();
        String collectionName = transformationContext.getCollectionName();
        String staticUrlPrefix = transformationContext.getWfs3Request()
                                                      .getStaticUrlPrefix();
        URICustomizer uriCustomizer = transformationContext.getWfs3Request()
                                                           .getUriCustomizer();
        FeatureCollectionView featureTypeDataset;

        boolean bare = transformationContext.getWfs3Request()
                                            .getUriCustomizer()
                                            .getQueryParams()
                                            .stream()
                                            .anyMatch(nameValuePair -> nameValuePair.getName()
                                                                                    .equals("bare") && nameValuePair.getValue()
                                                                                                                    .equals("true"));

        if (transformationContext.isFeatureCollection()) {
            featureTypeDataset = createFeatureCollectionView(serviceData.getFeatureTypes()
                                                                        .get(collectionName), uriCustomizer.copy(), serviceData.getFilterableFieldsForFeatureType(collectionName, true), serviceData.getHtmlNamesForFeatureType(collectionName), staticUrlPrefix, bare);

            addDatasetNavigation(featureTypeDataset, serviceData.getLabel(), serviceData.getFeatureTypes()
                                                                                        .get(collectionName)
                                                                                        .getLabel(), transformationContext.getLinks(), uriCustomizer.copy());
        } else {
            featureTypeDataset = createFeatureDetailsView(serviceData.getFeatureTypes()
                                                                     .get(collectionName), uriCustomizer.copy(), transformationContext.getLinks(), serviceData.getLabel(), uriCustomizer.getLastPathSegment(), staticUrlPrefix);
        }

        //TODO
        featureTypeDataset.hideMap = true;

        return Optional.of(new FeatureTransformerHtml(ImmutableFeatureTransformationContextHtml.builder()
                                                                                               .from(transformationContext)
                                                                                               .featureTypeDataset(featureTypeDataset)
                                                                                               .codelists(codelists)
                                                                                               .mustacheRenderer(dropwizard.getMustacheRenderer())
                                                                                               .build(), http.getDefaultClient()));
    }

    @Override
    public Optional<TargetMappingProviderFromGml> getMappingGenerator() {
        return Optional.of(new Gml2MicrodataMappingProvider());
    }

    private FeatureCollectionView createFeatureCollectionView(FeatureTypeConfigurationOgcApi featureType,
                                                              URICustomizer uriCustomizer,
                                                              Map<String, String> filterableFields,
                                                              Map<String, String> htmlNames, String staticUrlPrefix,
                                                              boolean bare) {
        URI requestUri = null;
        try {
            requestUri = uriCustomizer.build();
        } catch (URISyntaxException e) {
            //ignore
        }
        URICustomizer uriBuilder = uriCustomizer.copy()
                                                .clearParameters()
                                                .ensureParameter("f", MEDIA_TYPE.parameter())
                                                .ensureLastPathSegment("items");

        DatasetView dataset = new DatasetView("", requestUri, null, staticUrlPrefix, htmlConfig);

        FeatureCollectionView featureTypeDataset = new FeatureCollectionView(bare ? "featureCollectionBare" : "featureCollection", requestUri, featureType.getId(), featureType.getLabel(), staticUrlPrefix, htmlConfig);

        //TODO featureTypeDataset.uriBuilder = uriBuilder;
        dataset.featureTypes.add(featureTypeDataset);

        featureTypeDataset.temporalExtent = featureType.getExtent()
                                                       .getTemporal();

        BoundingBox bbox = featureType.getExtent()
                                      .getSpatial();
        featureTypeDataset.bbox2 = ImmutableMap.of("minLng", Double.toString(bbox.getYmin()), "minLat", Double.toString(bbox.getXmin()), "maxLng", Double.toString(bbox.getYmax()), "maxLat", Double.toString(bbox.getXmax()));

        featureTypeDataset.filterFields = filterableFields.entrySet()
                                                          .stream()
                                                          .peek(entry -> {
                                                              if (htmlNames.containsKey(entry.getValue())) {
                                                                  entry.setValue(htmlNames.get(entry.getValue()));
                                                              }
                                                          })
                                                          .collect(Collectors.toSet());
        featureTypeDataset.uriBuilder = uriBuilder;
        featureTypeDataset.uriBuilder2 = uriCustomizer.copy();

        //TODO: refactor all views, use extendable Wfs3Collection(s) as base, move this to Wfs3CollectionMetadataExtension
        featureTypeDataset.spatialSearch = featureType.getCapabilities()
                                                      .stream()
                                                      .anyMatch(extensionConfiguration -> Objects.equals(extensionConfiguration.getExtensionType(), "FILTER_TRANSFORMERS"));

        return featureTypeDataset;
    }

    private FeatureCollectionView createFeatureDetailsView(FeatureTypeConfigurationOgcApi featureType,
                                                           URICustomizer uriCustomizer, List<Wfs3Link> links,
                                                           String serviceLabel, String featureId,
                                                           String staticUrlPrefix) {
        URI requestUri = null;
        try {
            requestUri = uriCustomizer.build();
        } catch (URISyntaxException e) {
            //ignore
        }
        URICustomizer uriBuilder = uriCustomizer.copy()
                                                .clearParameters()
                                                .ensureParameter("f", MEDIA_TYPE.parameter())
                                                .removeLastPathSegments(1);

        FeatureCollectionView featureTypeDataset = new FeatureCollectionView("featureDetails", requestUri, featureType.getId(), featureType.getLabel(), staticUrlPrefix, htmlConfig);
        featureTypeDataset.description = featureType.getDescription()
                                                    .orElse(featureType.getLabel());

        featureTypeDataset.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Services", uriBuilder.copy()
                                                             .removePathSegment("collections", -3)
                                                             .removeLastPathSegments(3)
                                                             .toString()))
                .add(new NavigationDTO(serviceLabel, uriBuilder.copy()
                                                               .removePathSegment("collections", -3)
                                                               .removeLastPathSegments(2)
                                                               .toString()))
                .add(new NavigationDTO(featureType.getLabel(), uriBuilder.toString()))
                .add(new NavigationDTO(featureId))
                .build();

        featureTypeDataset.formats = links.stream()
                                          .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "alternate"))
                                          .sorted(Comparator.comparing(link -> link.getTypeLabel()
                                                                                   .toUpperCase()))
                                          .map(wfs3Link -> new NavigationDTO(wfs3Link.getTypeLabel(), wfs3Link.getHref()))
                                          .collect(Collectors.toList());

        featureTypeDataset.uriBuilder2 = uriCustomizer.copy();

        /*new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("GeoJson", "f=json"))
                .add(new NavigationDTO("GML", "f=xml"))
                .add(new NavigationDTO("JSON-LD", "f=jsonld"))
                .build();*/

        return featureTypeDataset;
    }

    private void addDatasetNavigation(FeatureCollectionView featureCollectionView, String serviceLabel,
                                      String collectionLabel, List<Wfs3Link> links, URICustomizer uriCustomizer) {
        URICustomizer uriBuilder = uriCustomizer
                .clearParameters()
                .ensureParameter("f", MEDIA_TYPE.parameter())
                .removePathSegment("items", -1)
                .removePathSegment("collections", -2);

        featureCollectionView.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Services", uriBuilder.copy()
                                                             .removeLastPathSegments(2)
                                                             .toString()))
                .add(new NavigationDTO(serviceLabel, uriBuilder.copy()
                                                               .removeLastPathSegments(1)
                                                               .toString()))
                .add(new NavigationDTO(collectionLabel))
                .build();

        // TODO: only activated formats
        featureCollectionView.formats = links.stream()
                                             .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "alternate"))
                                             .sorted(Comparator.comparing(link -> link.getTypeLabel()
                                                                                      .toUpperCase()))
                                             .map(wfs3Link -> new NavigationDTO(wfs3Link.getTypeLabel(), wfs3Link.getHref()))
                                             .collect(Collectors.toList());

        /*new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("GeoJson", "f=json"))
                .add(new NavigationDTO("GML", "f=xml"))
                .add(new NavigationDTO("JSON-LD", "f=jsonld"))
                .build();*/


    }
}
