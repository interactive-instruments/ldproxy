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
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.collections.domain.Collections;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsFormatExtension;
import de.ii.ldproxy.ogcapi.domain.CommonFormatExtension;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ConformanceDeclaration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.LandingPage;
import de.ii.ldproxy.ogcapi.domain.OgcApiApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.xtraplatform.akka.http.Http;
import de.ii.xtraplatform.codelists.CodelistRegistry;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.features.app.FeatureSchemaToTypeVisitor;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;
import de.ii.xtraplatform.stringtemplates.StringTemplateFilters;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;

import javax.ws.rs.core.MediaType;
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
    private final Schema schema = new StringSchema().example("<html>...</html>");
    private final static String schemaRef = "#/components/schemas/htmlSchema";

    @Context
    private BundleContext bc;

    @Requires
    private Dropwizard dropwizard;

    @Requires
    private CodelistRegistry codelistRegistry;

    @Requires
    private Http http;

    @Requires
    private KeyValueStore keyValueStore;

    @Requires
    private I18n i18n;

    @Requires
    private OgcApiFeatureCoreProviders providers;

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/html");
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
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(schemaRef)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, HtmlConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData.getCollections().get(collectionId), HtmlConfiguration.class);
    }

    private boolean isNoIndexEnabledForApi(OgcApiApiDataV2 apiData) {
        return apiData.getExtension(HtmlConfiguration.class)
                .map(HtmlConfiguration::getNoIndexEnabled)
                .orElse(true);
    }

    private boolean showCollectionDescriptionsInOverview(OgcApiApiDataV2 apiData) {
        return apiData.getExtension(HtmlConfiguration.class)
                .map(HtmlConfiguration::getCollectionDescriptionsInOverview)
                .orElse(false);
    }

    private HtmlConfiguration.LAYOUT getLayout(OgcApiApiDataV2 apiData) {
        return apiData.getExtension(HtmlConfiguration.class)
                .map(HtmlConfiguration::getLayout)
                .orElse(HtmlConfiguration.LAYOUT.CLASSIC);
    }

    @Override
    public Object getLandingPageEntity(LandingPage apiLandingPage,
                                           OgcApiApi api,
                                           OgcApiRequestContext requestContext) {

        String rootTitle = i18n.get("root", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(api.getData().getApiVersion().isPresent() ? 2 : 1)
                        .toString()))
                .add(new NavigationDTO(api.getData().getLabel()))
                .build();

        HtmlConfiguration htmlConfig = api.getData()
                                          .getExtension(HtmlConfiguration.class)
                                          .orElse(null);

        OgcApiLandingPageView landingPageView = new OgcApiLandingPageView(api.getData(), apiLandingPage, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), requestContext.getUriCustomizer(), i18n, requestContext.getLanguage());

        return landingPageView;
    }

    @Override
    public Object getConformanceEntity(ConformanceDeclaration conformanceDeclaration,
                                           OgcApiApi api, OgcApiRequestContext requestContext)  {

        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String conformanceDeclarationTitle = i18n.get("conformanceDeclarationTitle", requestContext.getLanguage());

        final URICustomizer uriCustomizer = requestContext.getUriCustomizer();
        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle,
                                       uriCustomizer.copy()
                                                     .removeLastPathSegments(api.getData().getApiVersion().isPresent() ? 3 : 2)
                                                     .toString()))
                .add(new NavigationDTO(api.getData().getLabel(),
                                       uriCustomizer.copy()
                                                    .removeLastPathSegments(1)
                                                    .toString()))
                .add(new NavigationDTO(conformanceDeclarationTitle))
                .build();

        HtmlConfiguration htmlConfig = api.getData()
                                          .getExtension(HtmlConfiguration.class)
                                          .orElse(null);

        OgcApiConformanceDeclarationView ogcApiConformanceDeclarationView =
                new OgcApiConformanceDeclarationView(conformanceDeclaration, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), i18n, requestContext.getLanguage());
        return ogcApiConformanceDeclarationView;
    }

    @Override
    public Object getCollectionsEntity(Collections collections, OgcApiApi api, OgcApiRequestContext requestContext) {

        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String collectionsTitle = i18n.get("collectionsTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(api.getData().getApiVersion().isPresent() ?  3 : 2)
                        .toString()))
                .add(new NavigationDTO(api.getData().getLabel(), requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(1)
                        .toString()))
                .add(new NavigationDTO(collectionsTitle))
                .build();

        HtmlConfiguration htmlConfig = api.getData()
                                          .getExtension(HtmlConfiguration.class)
                                          .orElse(null);

        OgcApiCollectionsView collectionsView = new OgcApiCollectionsView(api.getData(), collections, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), showCollectionDescriptionsInOverview(api.getData()), i18n, requestContext.getLanguage(), providers.getFeatureProvider(api.getData()).getData().getDataSourceUrl());

        return collectionsView;
    }


    @Override
    public Object getCollectionEntity(OgcApiCollection ogcApiCollection,
                                          OgcApiApi api,
                                          OgcApiRequestContext requestContext) {

        String rootTitle = i18n.get("root", requestContext.getLanguage());
        String collectionsTitle = i18n.get("collectionsTitle", requestContext.getLanguage());

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(api.getData().getApiVersion().isPresent() ? 4 : 3)
                        .toString()))
                .add(new NavigationDTO(api.getData().getLabel(), requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(2)
                        .toString()))
                .add(new NavigationDTO(collectionsTitle, requestContext.getUriCustomizer().copy()
                        .removeLastPathSegments(1)
                        .toString()))
                .add(new NavigationDTO(ogcApiCollection.getTitle().orElse(ogcApiCollection.getId())))
                .build();

        HtmlConfiguration htmlConfig = api.getData()
                                                 .getCollections()
                                                 .get(ogcApiCollection.getId())
                                                 .getExtension(HtmlConfiguration.class)
                                                 .orElse(null);

        OgcApiCollectionView collectionView = new OgcApiCollectionView(api.getData(), ogcApiCollection, breadCrumbs, requestContext.getStaticUrlPrefix(), htmlConfig, isNoIndexEnabledForApi(api.getData()), i18n, requestContext.getLanguage());

        return collectionView;
    }

    @Override
    public boolean canTransformFeatures() {
        return true;
    }

    @Override
    public Optional<FeatureTransformer2> getFeatureTransformer(FeatureTransformationContext transformationContext, Optional<Locale> language) {
        OgcApiApiDataV2 serviceData = transformationContext.getApiData();
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
            FeatureTypeConfigurationOgcApi collectionData = serviceData.getCollections()
                                                                       .get(collectionName);
            Optional<OgcApiFeaturesCoreConfiguration> featuresCoreConfiguration = collectionData.getExtension(OgcApiFeaturesCoreConfiguration.class);
            Optional<HtmlConfiguration> htmlConfiguration = collectionData.getExtension(HtmlConfiguration.class);
            FeatureProviderDataV2 providerData = providers.getFeatureProvider(serviceData, collectionData)
                                                          .getData();

            Map<String, String> filterableFields = featuresCoreConfiguration
                                                                 .map(OgcApiFeaturesCoreConfiguration::getOtherFilterParameters)
                                                                 .orElse(ImmutableMap.of());

            Map<String, String> htmlNames = new LinkedHashMap<>();
            if (featuresCoreConfiguration.isPresent()) {
                List<String> featureTypeIds = featuresCoreConfiguration.get().getFeatureTypes();
                if (featureTypeIds.isEmpty())
                    featureTypeIds = ImmutableList.of(collectionName);
                featureTypeIds.forEach(featureTypeId -> {
                     //TODO: add function to FeatureSchema instead of using Visitor
                    providerData.getTypes().get(featureTypeId).accept(new FeatureSchemaToTypeVisitor(featureTypeId)).getProperties().keySet().forEach(property -> htmlNames.putIfAbsent(property, property));
                 });

                //TODO: apply rename transformers
                //Map<String, List<FeaturePropertyTransformation>> transformations = htmlConfiguration.getTransformations();
            }

            featureTypeDataset = createFeatureCollectionView(serviceData.getCollections()
                                                                        .get(collectionName), uriCustomizer.copy(), filterableFields, htmlNames, staticUrlPrefix, bare, language, isNoIndexEnabledForApi(serviceData), getLayout(serviceData), providers.getFeatureProvider(serviceData));

            addDatasetNavigation(featureTypeDataset, serviceData.getLabel(), serviceData.getCollections()
                                                                                        .get(collectionName)
                                                                                        .getLabel(), transformationContext.getLinks(), uriCustomizer.copy(), language, serviceData.getApiVersion());
        } else {
            featureTypeDataset = createFeatureDetailsView(serviceData.getCollections()
                                                                     .get(collectionName), uriCustomizer.copy(), transformationContext.getLinks(), serviceData.getLabel(), uriCustomizer.getLastPathSegment(), staticUrlPrefix, language, isNoIndexEnabledForApi(serviceData), serviceData.getApiVersion(), getLayout(serviceData));
        }

        ImmutableFeatureTransformationContextHtml transformationContextHtml = ImmutableFeatureTransformationContextHtml.builder()
                .from(transformationContext)
                .featureTypeDataset(featureTypeDataset)
                .codelists(codelistRegistry.getCodelists())
                .mustacheRenderer(dropwizard.getMustacheRenderer())
                .i18n(i18n)
                .language(language)
                .build();

        FeatureTransformer2 transformer;
        switch (getLayout(serviceData)) {
            default:
            case CLASSIC:
                transformer = new FeatureTransformerHtml(transformationContextHtml, http.getDefaultClient());
                break;

            case COMPLEX_OBJECTS:
                transformer = new FeatureTransformerHtmlComplexObjects(transformationContextHtml, http.getDefaultClient());
                break;
        }

        return Optional.of(transformer);
    }

    @Override
    public Optional<TargetMappingProviderFromGml> getMappingGenerator() {
        return Optional.of(new Gml2MicrodataMappingProvider());
    }

    private FeatureCollectionView createFeatureCollectionView(FeatureTypeConfigurationOgcApi featureType,
                                                              URICustomizer uriCustomizer,
                                                              Map<String, String> filterableFields,
                                                              Map<String, String> htmlNames, String staticUrlPrefix,
                                                              boolean bare, Optional<Locale> language,
                                                              boolean noIndex,
                                                              HtmlConfiguration.LAYOUT layout,
                                                              FeatureProvider2 featureProvider) {
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

        HtmlConfiguration htmlConfig = featureType.getExtension(HtmlConfiguration.class)
                                                 .orElse(null);

        DatasetView dataset = new DatasetView("", requestUri, null, staticUrlPrefix, htmlConfig, noIndex);

        FeatureCollectionView featureTypeDataset = new FeatureCollectionView(bare ? "featureCollectionBare" : "featureCollection", requestUri, featureType.getId(), featureType.getLabel(), featureType.getDescription().orElse(null), staticUrlPrefix, htmlConfig, null, noIndex, i18n, language.orElse(Locale.ENGLISH), layout);

        dataset.featureTypes.add(featureTypeDataset);

        boolean hasExtent = featureType.getExtent().isPresent();
        if (hasExtent) {
            featureTypeDataset.temporalExtent = featureType.getExtent().get()
                    .getTemporal().orElse(null);

            featureType.getExtent().get().getSpatial().ifPresent(bbox -> featureTypeDataset.bbox2 = ImmutableMap.of("minLng", Double.toString(bbox.getXmin()), "minLat", Double.toString(bbox.getYmin()), "maxLng", Double.toString(bbox.getXmax()), "maxLat", Double.toString(bbox.getYmax())));
        } else {
            featureTypeDataset.temporalExtent = null;
        }

        featureTypeDataset.filterFields = filterableFields.entrySet()
                                                          .stream()
                                                          .map(entry -> {
                                                              if (htmlNames.containsKey(entry.getValue())) {
                                                                  //entry.setValue(htmlNames.get(entry.getValue()));
                                                                  return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), htmlNames.get(entry.getValue()));
                                                              }
                                                              return entry;
                                                          })
                                                          .collect(ImmutableSet.toImmutableSet());
        featureTypeDataset.uriBuilder = uriCustomizer.copy()
                                                     .ensureParameter("f", MEDIA_TYPE.parameter());
        featureTypeDataset.uriBuilderWithFOnly = uriCustomizer.copy()
                                                              .clearParameters()
                                                              .ensureParameter("f", MEDIA_TYPE.parameter());

        //TODO: refactor all views, use extendable OgcApiCollection(s) as base, move this to OgcApiCollectionExtension
        featureTypeDataset.spatialSearch = featureType.getExtensions()
                                                      .stream()
                                                      .anyMatch(extensionConfiguration -> Objects.equals(extensionConfiguration.getBuildingBlock(), "FILTER_TRANSFORMERS"));

        return featureTypeDataset;
    }

    private FeatureCollectionView createFeatureDetailsView(FeatureTypeConfigurationOgcApi featureType,
                                                           URICustomizer uriCustomizer, List<OgcApiLink> links,
                                                           String apiLabel, String featureId,
                                                           String staticUrlPrefix, Optional<Locale> language,
                                                           boolean noIndex,
                                                           Optional<Integer> apiVersion,
                                                           HtmlConfiguration.LAYOUT layout) {

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

        Optional<String> template = featureType.getPersistentUriTemplate();
        String persistentUri = null;
        if (template.isPresent()) {
            // we have a template and need to replace the local feature id
            persistentUri = StringTemplateFilters.applyTemplate(template.get(), featureId);
        }

        HtmlConfiguration htmlConfig = featureType.getExtension(HtmlConfiguration.class)
                                                  .orElse(null);

        FeatureCollectionView featureTypeDataset = new FeatureCollectionView("featureDetails", requestUri, featureType.getId(), featureType.getLabel(), featureType.getDescription().orElse(null), staticUrlPrefix, htmlConfig, persistentUri, noIndex, i18n, language.orElse(Locale.ENGLISH), layout);
        featureTypeDataset.description = featureType.getDescription()
                                                    .orElse(featureType.getLabel());

        featureTypeDataset.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, uriBuilder.copy()
                        .removeLastPathSegments(apiVersion.isPresent() ? 5 : 4)
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

        featureTypeDataset.uriBuilder = uriCustomizer.copy();

        return featureTypeDataset;
    }

    private void addDatasetNavigation(FeatureCollectionView featureCollectionView, String apiLabel,
                                      String collectionLabel, List<OgcApiLink> links, URICustomizer uriCustomizer,
                                      Optional<Locale> language, Optional<Integer> apiVersion) {

        String rootTitle = i18n.get("root", language);
        String collectionsTitle = i18n.get("collectionsTitle", language);
        String itemsTitle = i18n.get("itemsTitle", language);

        URICustomizer uriBuilder = uriCustomizer
                .clearParameters()
                .removePathSegment("items", -1);

        featureCollectionView.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, uriBuilder.copy()
                        .removeLastPathSegments(apiVersion.isPresent() ? 4 : 3)
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
