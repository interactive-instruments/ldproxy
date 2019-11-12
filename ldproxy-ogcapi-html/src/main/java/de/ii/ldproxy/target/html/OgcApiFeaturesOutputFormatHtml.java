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
import de.ii.ldproxy.codelists.CodelistEntity;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.Collections;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension;
import de.ii.xtraplatform.akka.http.Http;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class OgcApiFeaturesOutputFormatHtml implements ConformanceClass, CollectionsFormatExtension, CommonFormatExtension, OgcApiFeatureFormatExtension {

    static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .label("HTML")
            .parameter("html")
            .build();
    public static final OgcApiMediaType COLLECTION_MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .label("HTML")
            .parameter("html")
            .build();

    @Context
    private BundleContext bc;

    @Requires
    private HtmlConfig htmlConfig;

    @Requires
    private Dropwizard dropwizard;

    @Requires(optional = true)
    private CodelistEntity[] codelists;

    @Requires
    private Http http;

    @Requires
    private KeyValueStore keyValueStore;

    @Requires
    private I18n i18n;

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/html";
    }

    @Override
    public String getPathPattern() {
        return "^/?(?:conformance|collections(?:/[\\w\\-]+(?:/items(?:/[^/\\s]+)?)?)?)?$";
    }

    @Override
    public OgcApiMediaType getCollectionMediaType() {
        return COLLECTION_MEDIA_TYPE;
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, HtmlConfiguration.class);
    }

    @Override
    public Response getLandingPageResponse(LandingPage apiLandingPage,
                                           OgcApiDataset api,
                                           OgcApiRequestContext requestContext) {

        String rootTitle = i18n.get("root", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(1)
                        .toString()))
                .add(new NavigationDTO(api.getData().getLabel()))
                .build();

        OgcApiLandingPageView landingPageView = new OgcApiLandingPageView(api.getData(), apiLandingPage, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());

        return Response.ok()
                .type(getMediaType().type())
                .entity(landingPageView)
                .build();
    }

    @Override
    public Response getConformanceResponse(ConformanceDeclaration conformanceDeclaration,
                                           OgcApiDataset api, OgcApiRequestContext requestContext)  {

        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String conformanceDeclarationTitle = i18n.get("conformanceDeclarationTitle", requestContext.getLanguage());

        final URICustomizer uriCustomizer = requestContext.getUriCustomizer();
        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle,
                                       uriCustomizer.copy()
                                                     .removeLastPathSegments(2)
                                                     .toString()))
                .add(new NavigationDTO(api.getData().getLabel(),
                                       uriCustomizer.copy()
                                                    .removeLastPathSegments(1)
                                                    .toString()))
                .add(new NavigationDTO(conformanceDeclarationTitle))
                .build();

        OgcApiConformanceDeclarationView ogcApiConformanceDeclarationView =
                new OgcApiConformanceDeclarationView(conformanceDeclaration, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, i18n, requestContext.getLanguage());
        return Response.ok()
                       .type(getMediaType().type())
                       .entity(ogcApiConformanceDeclarationView)
                       .build();
    }

    @Override
    public Response getCollectionsResponse(Collections collections, OgcApiDataset api, OgcApiRequestContext requestContext) {

        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String collectionsTitle = i18n.get("collectionsTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(2)
                        .toString()))
                .add(new NavigationDTO(api.getData().getLabel(), requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(1)
                        .toString()))
                .add(new NavigationDTO(collectionsTitle))
                .build();

        OgcApiCollectionsView collectionsView = new OgcApiCollectionsView(api.getData(), collections, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, i18n, requestContext.getLanguage());

        return Response.ok()
                .type(getMediaType().type())
                .entity(collectionsView)
                .build();
    }


    @Override
    public Response getCollectionResponse(OgcApiCollection ogcApiCollection,
                                          OgcApiDataset api,
                                          OgcApiRequestContext requestContext) {

        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String collectionsTitle = i18n.get("collectionsTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(3)
                        .toString()))
                .add(new NavigationDTO(api.getData().getLabel(), requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(2)
                        .toString()))
                .add(new NavigationDTO(collectionsTitle, requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(1)
                        .toString()))
                .add(new NavigationDTO(ogcApiCollection.getTitle().orElse(ogcApiCollection.getId())))
                .build();

        OgcApiCollectionView collectionView = new OgcApiCollectionView(api.getData(), ogcApiCollection, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, i18n, requestContext.getLanguage());

        return Response.ok()
                .type(getMediaType().type())
                .entity(collectionView)
                .build();
    }

    @Override
    public boolean canTransformFeatures() {
        return true;
    }

    @Override
    public Optional<FeatureTransformer> getFeatureTransformer(FeatureTransformationContext transformationContext, Optional<Locale> language) {
        OgcApiDatasetData serviceData = transformationContext.getApiData();
        String collectionName = transformationContext.getCollectionId();
        String staticUrlPrefix = transformationContext.getOgcApiRequest()
                                                      .getStaticUrlPrefix();
        URICustomizer uriCustomizer = transformationContext.getOgcApiRequest()
                                                           .getUriCustomizer();
        FeatureCollectionView featureTypeDataset;

        boolean bare = transformationContext.getOgcApiRequest()
                                            .getUriCustomizer()
                                            .getQueryParams()
                                            .stream()
                                            .anyMatch(nameValuePair -> nameValuePair.getName()
                                                                                    .equals("bare") && nameValuePair.getValue()
                                                                                                                    .equals("true"));

        if (transformationContext.isFeatureCollection()) {
            featureTypeDataset = createFeatureCollectionView(serviceData.getFeatureTypes()
                                                                        .get(collectionName), uriCustomizer.copy(), serviceData.getFilterableFieldsForFeatureType(collectionName, true), serviceData.getHtmlNamesForFeatureType(collectionName), staticUrlPrefix, bare, language);

            addDatasetNavigation(featureTypeDataset, serviceData.getLabel(), serviceData.getFeatureTypes()
                                                                                        .get(collectionName)
                                                                                        .getLabel(), transformationContext.getLinks(), uriCustomizer.copy(), language);
        } else {
            featureTypeDataset = createFeatureDetailsView(serviceData.getFeatureTypes()
                                                                     .get(collectionName), uriCustomizer.copy(), transformationContext.getLinks(), serviceData.getLabel(), uriCustomizer.getLastPathSegment(), staticUrlPrefix, language);
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
                                                              boolean bare, Optional<Locale> language) {
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

        FeatureCollectionView featureTypeDataset = new FeatureCollectionView(bare ? "featureCollectionBare" : "featureCollection", requestUri, featureType.getId(), featureType.getLabel(), featureType.getDescription().orElse(null), staticUrlPrefix, htmlConfig, i18n, language.orElse(Locale.ENGLISH));

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

        //TODO: refactor all views, use extendable OgcApiCollection(s) as base, move this to OgcApiCollectionExtension
        featureTypeDataset.spatialSearch = featureType.getCapabilities()
                                                      .stream()
                                                      .anyMatch(extensionConfiguration -> Objects.equals(extensionConfiguration.getExtensionType(), "FILTER_TRANSFORMERS"));

        return featureTypeDataset;
    }

    private FeatureCollectionView createFeatureDetailsView(FeatureTypeConfigurationOgcApi featureType,
                                                           URICustomizer uriCustomizer, List<OgcApiLink> links,
                                                           String apiLabel, String featureId,
                                                           String staticUrlPrefix, Optional<Locale> language) {

        String rootTitle = i18n.get("root", language);
        String collectionsTitle = i18n.get("collectionsTitle", language);
        String itemsTitle = i18n.get("itemsTitle", language);

        URI requestUri = null;
        try {
            requestUri = uriCustomizer.build();
        } catch (URISyntaxException e) {
            // ignore
        }
        URICustomizer uriBuilder = uriCustomizer.copy()
                                                .clearParameters()
                                                .removeLastPathSegments(1);

        FeatureCollectionView featureTypeDataset = new FeatureCollectionView("featureDetails", requestUri, featureType.getId(), featureType.getLabel(), featureType.getDescription().orElse(null), staticUrlPrefix, htmlConfig, i18n, language.orElse(Locale.ENGLISH));
        featureTypeDataset.description = featureType.getDescription()
                                                    .orElse(featureType.getLabel());

        featureTypeDataset.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, uriBuilder.copy()
                        .removeLastPathSegments(4)
                        .toString()))
                .add(new NavigationDTO(apiLabel, uriBuilder.copy()
                        .removeLastPathSegments(3)
                        .toString()))
                .add(new NavigationDTO(collectionsTitle, uriBuilder.copy()
                        .removeLastPathSegments(2)
                        .toString()))
                .add(new NavigationDTO(featureType.getLabel(), uriBuilder.copy()
                        .removeLastPathSegments(1)
                        .toString()))
                .add(new NavigationDTO(itemsTitle, uriBuilder.toString()))
                .add(new NavigationDTO(featureId))
                .build();

        featureTypeDataset.formats = links.stream()
                                          .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "alternate"))
                                          .sorted(Comparator.comparing(link -> link.getTypeLabel()
                                                                                   .toUpperCase()))
                                          .map(wfs3Link -> new NavigationDTO(wfs3Link.getTypeLabel(), wfs3Link.getHref()))
                                          .collect(Collectors.toList());

        featureTypeDataset.uriBuilder2 = uriCustomizer.copy();

        return featureTypeDataset;
    }

    private void addDatasetNavigation(FeatureCollectionView featureCollectionView, String apiLabel,
                                      String collectionLabel, List<OgcApiLink> links, URICustomizer uriCustomizer,
                                      Optional<Locale> language) {

        String rootTitle = i18n.get("root", language);
        String collectionsTitle = i18n.get("collectionsTitle", language);
        String itemsTitle = i18n.get("itemsTitle", language);

        URICustomizer uriBuilder = uriCustomizer
                .clearParameters()
                .removePathSegment("items", -1);

        featureCollectionView.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, uriBuilder.copy()
                        .removeLastPathSegments(3)
                        .toString()))
                .add(new NavigationDTO(apiLabel, uriBuilder.copy()
                        .removeLastPathSegments(2)
                        .toString()))
                .add(new NavigationDTO(collectionsTitle, uriBuilder.copy()
                        .removeLastPathSegments(1)
                        .toString()))
                .add(new NavigationDTO(collectionLabel, uriBuilder.toString()))
                .add(new NavigationDTO(itemsTitle))
                .build();

        featureCollectionView.formats = links.stream()
                                             .filter(link -> Objects.equals(link.getRel(), "alternate"))
                                             .sorted(Comparator.comparing(link -> link.getTypeLabel()
                                                                                      .toUpperCase()))
                                             .map(link -> new NavigationDTO(link.getTypeLabel(), link.getHref()))
                                             .collect(Collectors.toList());
    }
}
