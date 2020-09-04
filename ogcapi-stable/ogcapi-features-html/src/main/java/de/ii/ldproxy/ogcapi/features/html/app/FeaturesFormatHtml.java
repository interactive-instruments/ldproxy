/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.dropwizard.domain.Dropwizard;
import de.ii.xtraplatform.features.domain.FeatureSchemaToTypeVisitor;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.streams.domain.Http;
import de.ii.xtraplatform.stringtemplates.domain.StringTemplateFilters;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.*;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class FeaturesFormatHtml implements ConformanceClass, FeatureFormatExtension {

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .label("HTML")
            .parameter("html")
            .build();
    public static final ApiMediaType COLLECTION_MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .label("HTML")
            .parameter("html")
            .build();
    private final Schema schema = new StringSchema().example("<html>...</html>");
    private final static String schemaRef = "#/components/schemas/htmlSchema";

    @Requires
    private Dropwizard dropwizard;

    @Requires
    private EntityRegistry entityRegistry;

    @Requires
    private Http http;

    @Requires
    private I18n i18n;

    @Requires
    private FeaturesCoreProviders providers;

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/html");
    }

    @Override
    public String getPathPattern() {
        return "^/?(?:conformance|collections/[\\w\\-]+/items(?:/[^/\\s]+)?)?$";
    }

    @Override
    public ApiMediaType getCollectionMediaType() {
        return COLLECTION_MEDIA_TYPE;
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(schemaRef)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesHtmlConfiguration.class;
    }

    private boolean isNoIndexEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(HtmlConfiguration.class)
                .map(HtmlConfiguration::getNoIndexEnabled)
                .orElse(true);
    }

    private FeaturesHtmlConfiguration.LAYOUT getLayout(OgcApiDataV2 apiData) {
        return apiData.getExtension(FeaturesHtmlConfiguration.class)
                .map(FeaturesHtmlConfiguration::getLayout)
                .orElse(FeaturesHtmlConfiguration.LAYOUT.CLASSIC);
    }

    @Override
    public boolean canTransformFeatures() {
        return true;
    }

    @Override
    public Optional<FeatureTransformer2> getFeatureTransformer(FeatureTransformationContext transformationContext, Optional<Locale> language) {
        OgcApiDataV2 serviceData = transformationContext.getApiData();
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
            Optional<FeaturesCoreConfiguration> featuresCoreConfiguration = collectionData.getExtension(FeaturesCoreConfiguration.class);
            Optional<HtmlConfiguration> htmlConfiguration = collectionData.getExtension(HtmlConfiguration.class);
            FeatureProviderDataV2 providerData = providers.getFeatureProvider(serviceData, collectionData)
                                                          .getData();

            Map<String, String> filterableFields = featuresCoreConfiguration
                                                                 .map(FeaturesCoreConfiguration::getOtherFilterParameters)
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

            featureTypeDataset = createFeatureCollectionView(serviceData, serviceData.getCollections()
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
                .codelists(entityRegistry.getEntitiesForType(Codelist.class)
                                         .stream()
                                         .collect(Collectors.toMap(c -> c.getId(), c -> c)))
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

    private FeatureCollectionView createFeatureCollectionView(OgcApiDataV2 apiData,
                                                              FeatureTypeConfigurationOgcApi featureType,
                                                              URICustomizer uriCustomizer,
                                                              Map<String, String> filterableFields,
                                                              Map<String, String> htmlNames, String staticUrlPrefix,
                                                              boolean bare, Optional<Locale> language,
                                                              boolean noIndex,
                                                              FeaturesHtmlConfiguration.LAYOUT layout,
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

        FeatureCollectionView featureTypeDataset = new FeatureCollectionView(bare ? "featureCollectionBare" : "featureCollection", requestUri, featureType.getId(), featureType.getLabel(), featureType.getDescription().orElse(null), staticUrlPrefix, htmlConfig, null, noIndex, i18n, language.orElse(Locale.ENGLISH), layout);

        featureTypeDataset.temporalExtent = apiData.getTemporalExtent(featureType.getId()).orElse(null);
        apiData.getSpatialExtent(featureType.getId()).ifPresent(bbox -> featureTypeDataset.bbox2 = ImmutableMap.of("minLng", Double.toString(bbox.getXmin()), "minLat", Double.toString(bbox.getYmin()), "maxLng", Double.toString(bbox.getXmax()), "maxLat", Double.toString(bbox.getYmax())));

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

        //TODO: refactor all views, use extendable OgcApiCollection(s) as base, move this to CollectionExtension
        featureTypeDataset.spatialSearch = featureType.getExtensions()
                                                      .stream()
                                                      .anyMatch(extensionConfiguration -> Objects.equals(extensionConfiguration.getBuildingBlock(), "FILTER_TRANSFORMERS"));

        return featureTypeDataset;
    }

    private FeatureCollectionView createFeatureDetailsView(FeatureTypeConfigurationOgcApi featureType,
                                                           URICustomizer uriCustomizer, List<Link> links,
                                                           String apiLabel, String featureId,
                                                           String staticUrlPrefix, Optional<Locale> language,
                                                           boolean noIndex,
                                                           Optional<Integer> apiVersion,
                                                           FeaturesHtmlConfiguration.LAYOUT layout) {

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
                                          .filter(link -> Objects.equals(link.getRel(), "alternate"))
                                          .sorted(Comparator.comparing(link -> link.getTypeLabel()
                                                                                   .toUpperCase()))
                                          .map(link -> new NavigationDTO(link.getTypeLabel(), link.getHref()))
                                          .collect(Collectors.toList());

        featureTypeDataset.uriBuilder = uriCustomizer.copy();

        return featureTypeDataset;
    }

    private void addDatasetNavigation(FeatureCollectionView featureCollectionView, String apiLabel,
                                      String collectionLabel, List<Link> links, URICustomizer uriCustomizer,
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
