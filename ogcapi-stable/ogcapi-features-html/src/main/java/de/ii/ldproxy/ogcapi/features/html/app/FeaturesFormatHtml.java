/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.app;

import static de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeatureOpenApi.DEFAULT_FLATTENING_SEPARATOR;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionDynamicMetadataRegistry;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.Metadata;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreValidation;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration.POSITION;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.MapClient;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.dropwizard.domain.Dropwizard;
import de.ii.xtraplatform.dropwizard.domain.MustacheRenderer;
import de.ii.xtraplatform.dropwizard.domain.XtraPlatform;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation.Builder;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.features.domain.transform.WithTransformationsApplied;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import de.ii.xtraplatform.streams.domain.Http;
import de.ii.xtraplatform.stringtemplates.domain.StringTemplateFilters;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

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
    private final static WithTransformationsApplied SCHEMA_FLATTENER = new WithTransformationsApplied(
        ImmutableMap.of(
            PropertyTransformations.WILDCARD, new Builder().flatten(DEFAULT_FLATTENING_SEPARATOR).build()));

    private final Dropwizard dropwizard;
    private final EntityRegistry entityRegistry;
    private final Http http;
    private final I18n i18n;
    private final FeaturesCoreProviders providers;
    private final FeaturesCoreValidation featuresCoreValidator;
    private final CollectionDynamicMetadataRegistry metadataRegistry;
    private final XtraPlatform xtraPlatform;

    public FeaturesFormatHtml(@Requires Dropwizard dropwizard, @Requires EntityRegistry entityRegistry,
                              @Requires Http http, @Requires I18n i18n, @Requires FeaturesCoreProviders providers,
                              @Requires FeaturesCoreValidation featuresCoreValidator,
                              @Requires CollectionDynamicMetadataRegistry metadataRegistry,
                              @Requires XtraPlatform xtraPlatform) {
        this.dropwizard = dropwizard;
        this.entityRegistry = entityRegistry;
        this.http = http;
        this.i18n = i18n;
        this.providers = providers;
        this.featuresCoreValidator = featuresCoreValidator;
        this.metadataRegistry = metadataRegistry;
        this.xtraPlatform = xtraPlatform;
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/html", "http://www.opengis.net/spec/ogcapi-records-1/0.0/conf/html");
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
    public ValidationResult onStartup(OgcApiDataV2 apiData, MODE apiValidation) {

        // no additional operational checks for now, only validation; we can stop, if no validation is requested
        if (apiValidation== MODE.NONE)
            return ValidationResult.of();

        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
                .mode(apiValidation);

        Map<String, FeatureSchema> featureSchemas = providers.getFeatureSchemas(apiData);

        // get HTML configurations to process
        Map<String, FeaturesHtmlConfiguration> htmlConfigurationMap = apiData.getCollections()
                                                                  .entrySet()
                                                                  .stream()
                                                                  .map(entry -> {
                                                                      final FeatureTypeConfigurationOgcApi collectionData = entry.getValue();
                                                                      final FeaturesHtmlConfiguration config = collectionData.getExtension(FeaturesHtmlConfiguration.class).orElse(null);
                                                                      if (Objects.isNull(config))
                                                                          return null;
                                                                      return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), config);
                                                                  })
                                                                  .filter(Objects::nonNull)
                                                                  .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, Collection<String>> keyMap = htmlConfigurationMap.entrySet()
                                                                      .stream()
                                                                      .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()
                                                                                                                                                .getTransformations()
                                                                                                                                                .keySet()))
                                                                      .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Map.Entry<String, Collection<String>> stringCollectionEntry : featuresCoreValidator.getInvalidPropertyKeys(keyMap, featureSchemas).entrySet()) {
            for (String property : stringCollectionEntry.getValue()) {
                builder.addStrictErrors(MessageFormat.format("A transformation for property ''{0}'' in collection ''{1}'' is invalid, because the property was not found in the provider schema.", property, stringCollectionEntry.getKey()));
            }
        }

        Set<String> codelists = entityRegistry.getEntitiesForType(Codelist.class)
                                              .stream()
                                              .map(Codelist::getId)
                                              .collect(Collectors.toUnmodifiableSet());
        for (Map.Entry<String, FeaturesHtmlConfiguration> entry : htmlConfigurationMap.entrySet()) {
            String collectionId = entry.getKey();
            for (Map.Entry<String, List<PropertyTransformation>> entry2 : entry.getValue().getTransformations().entrySet()) {
                String property = entry2.getKey();
                for (PropertyTransformation transformation: entry2.getValue()) {
                    builder = transformation.validate(builder, collectionId, property, codelists);
                }
            }
        }

        return builder.build();
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

    private FeaturesHtmlConfiguration.POSITION getMapPosition(OgcApiDataV2 apiData, String collectionId) {
        return apiData.getExtension(FeaturesHtmlConfiguration.class, collectionId)
                .map(FeaturesHtmlConfiguration::getMapPosition)
                .orElse(FeaturesHtmlConfiguration.POSITION.AUTO);
    }

    private List<String> getGeometryProperties(OgcApiDataV2 apiData, String collectionId) {
        return apiData.getExtension(FeaturesHtmlConfiguration.class, collectionId)
            .map(FeaturesHtmlConfiguration::getGeometryProperties)
            .orElse(ImmutableList.of());
    }

    @Override
    public boolean canEncodeFeatures() {
        return true;
    }

    @Override
    public Optional<FeatureTokenEncoder<?>> getFeatureEncoder(
        FeatureTransformationContext transformationContext, Optional<Locale> language) {
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

            Integer htmlMaxLimit = collectionData.getExtension(FeaturesHtmlConfiguration.class)
                .map(FeaturesHtmlConfiguration::getMaximumPageSize)
                .orElse(null);
            if (Objects.nonNull(htmlMaxLimit) && htmlMaxLimit < transformationContext.getLimit())
                throw new IllegalArgumentException(String.format("The HTML output has a maximum page size (parameter 'limit') of %d. Found: %d",
                                                                 htmlMaxLimit, transformationContext.getLimit()));

            Optional<FeaturesCoreConfiguration> featuresCoreConfiguration = collectionData.getExtension(FeaturesCoreConfiguration.class);

            List<String> queryables = featuresCoreConfiguration
                .map(FeaturesCoreConfiguration::getQOrOtherFilterParameters)
                .orElse(ImmutableList.of());
            Map<String, String> filterableFields = transformationContext.getFeatureSchema()
                .map(schema -> schema.accept(SCHEMA_FLATTENER))
                .map(schema -> schema.getProperties().stream()
                    .filter(property -> queryables.contains(property.getName()))
                    .map(property -> new SimpleImmutableEntry<>(property.getName(),
                        property.getLabel().orElse(property.getName())))
                    .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue)))
                .orElse(ImmutableMap.of());

            featureTypeDataset = createFeatureCollectionView(serviceData, serviceData.getCollections()
                .get(collectionName), uriCustomizer.copy(), filterableFields, staticUrlPrefix, bare, language, isNoIndexEnabledForApi(serviceData), getMapPosition(serviceData, collectionName), getGeometryProperties(serviceData, collectionName));

            addDatasetNavigation(featureTypeDataset, serviceData.getLabel(), serviceData.getCollections()
                .get(collectionName)
                .getLabel(), transformationContext.getLinks(), uriCustomizer.copy(), language, serviceData.getSubPath());
        } else {
            featureTypeDataset = createFeatureDetailsView(serviceData, serviceData.getCollections()
                .get(collectionName), uriCustomizer.copy(), transformationContext.getLinks(), serviceData.getLabel(), uriCustomizer.getLastPathSegment(), staticUrlPrefix, language, isNoIndexEnabledForApi(serviceData), serviceData.getSubPath(), getMapPosition(serviceData, collectionName), getGeometryProperties(serviceData, collectionName));
        }

        ImmutableFeatureTransformationContextHtml transformationContextHtml = ImmutableFeatureTransformationContextHtml.builder()
            .from(transformationContext)
            .collectionView(featureTypeDataset)
            .codelists(entityRegistry.getEntitiesForType(Codelist.class)
                .stream()
                .collect(Collectors.toMap(c -> c.getId(), c -> c)))
            .mustacheRenderer((MustacheRenderer) dropwizard.getMustacheRenderer())
            .i18n(i18n)
            .language(language)
            .build();

        return Optional.of(new FeatureEncoderHtml(transformationContextHtml));
    }

    private FeatureCollectionView createFeatureCollectionView(OgcApiDataV2 apiData,
        FeatureTypeConfigurationOgcApi featureType,
        URICustomizer uriCustomizer,
        Map<String, String> filterableFields,
        String staticUrlPrefix,
        boolean bare, Optional<Locale> language,
        boolean noIndex,
        POSITION mapPosition,
        List<String> geometryProperties) {
        URI requestUri = null;
        try {
            requestUri = uriCustomizer.build();
        } catch (URISyntaxException e) {
            //ignore
        }

        Optional<BoundingBox> optionalBbox = metadataRegistry.getSpatialExtent(apiData.getId(), featureType.getId());

        Optional<HtmlConfiguration> htmlConfig = featureType.getExtension(HtmlConfiguration.class);
        String attribution = apiData.getMetadata().flatMap(Metadata::getAttribution).orElse(null);

        Optional<FeaturesHtmlConfiguration> config = featureType.getExtension(FeaturesHtmlConfiguration.class);
        MapClient.Type mapClientType = config.map(FeaturesHtmlConfiguration::getMapClientType)
                                             .orElse(MapClient.Type.MAP_LIBRE);
        String serviceUrl = new URICustomizer(xtraPlatform.getServicesUri()).ensureLastPathSegments(apiData.getSubPath().toArray(String[]::new)).toString();
        String styleUrl = htmlConfig.map(cfg -> cfg.getStyle(config.map(FeaturesHtmlConfiguration::getStyle), Optional.of(featureType.getId()), serviceUrl))
                                    .orElse(null);
        boolean removeZoomLevelConstraints = config.map(FeaturesHtmlConfiguration::getRemoveZoomLevelConstraints)
                                                   .orElse(false);

        FeatureCollectionView featureTypeDataset = new FeatureCollectionView(apiData, featureType, optionalBbox.orElse(null), bare ? "featureCollectionBare" : "featureCollection", requestUri, featureType.getId(), featureType.getLabel(), featureType.getDescription().orElse(null), attribution, staticUrlPrefix, htmlConfig.orElse(null), null, noIndex, i18n, language.orElse(Locale.ENGLISH), mapPosition, mapClientType, styleUrl, removeZoomLevelConstraints, filterableFields, geometryProperties);

        featureTypeDataset.temporalExtent = metadataRegistry.getTemporalExtent(apiData.getId(), featureType.getId()).orElse(null);
        optionalBbox.ifPresent(bbox -> featureTypeDataset.bbox = ImmutableMap.of("minLng", Double.toString(bbox.getXmin()), "minLat", Double.toString(bbox.getYmin()), "maxLng", Double.toString(bbox.getXmax()), "maxLat", Double.toString(bbox.getYmax())));

        featureTypeDataset.uriBuilder = uriCustomizer.copy()
                                                     .ensureParameter("f", MEDIA_TYPE.parameter());
        featureTypeDataset.uriBuilderWithFOnly = uriCustomizer.copy()
                                                              .clearParameters()
                                                              .ensureParameter("f", MEDIA_TYPE.parameter());

        return featureTypeDataset;
    }

    private FeatureCollectionView createFeatureDetailsView(OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureType,
                                                           URICustomizer uriCustomizer, List<Link> links,
                                                           String apiLabel, String featureId,
                                                           String staticUrlPrefix, Optional<Locale> language,
                                                           boolean noIndex,
                                                           List<String> subPathToLandingPage,
                                                           FeaturesHtmlConfiguration.POSITION mapPosition,
                                                           List<String> geometryProperties) {

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

        Optional<BoundingBox> optionalBbox = metadataRegistry.getSpatialExtent(apiData.getId(), featureType.getId());

        Optional<HtmlConfiguration> htmlConfig = featureType.getExtension(HtmlConfiguration.class);
        String attribution = apiData.getMetadata().flatMap(Metadata::getAttribution).orElse(null);

        Optional<FeaturesHtmlConfiguration> config = featureType.getExtension(FeaturesHtmlConfiguration.class);
        MapClient.Type mapClientType = config.map(FeaturesHtmlConfiguration::getMapClientType)
                                             .orElse(MapClient.Type.MAP_LIBRE);
        String serviceUrl = new URICustomizer(xtraPlatform.getServicesUri()).ensureLastPathSegments(apiData.getSubPath().toArray(String[]::new)).toString();
        String styleUrl = htmlConfig.map(cfg -> cfg.getStyle(config.map(FeaturesHtmlConfiguration::getStyle), Optional.of(featureType.getId()), serviceUrl))
                                    .orElse(null);
        boolean removeZoomLevelConstraints = config.map(FeaturesHtmlConfiguration::getRemoveZoomLevelConstraints)
                                                   .orElse(false);

        FeatureCollectionView featureTypeDataset = new FeatureCollectionView(apiData, featureType, optionalBbox.orElse(null), "featureDetails", requestUri, featureType.getId(), featureType.getLabel(), featureType.getDescription().orElse(null), attribution, staticUrlPrefix, htmlConfig.orElse(null), persistentUri, noIndex, i18n, language.orElse(Locale.ENGLISH), mapPosition, mapClientType, styleUrl, removeZoomLevelConstraints, null, geometryProperties);
        featureTypeDataset.description = featureType.getDescription()
                                                    .orElse(featureType.getLabel());

        featureTypeDataset.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, uriBuilder.copy()
                        .removeLastPathSegments(3 + subPathToLandingPage.size())
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
                                      Optional<Locale> language, List<String> subPathToLandingPage) {

        String rootTitle = i18n.get("root", language);
        String collectionsTitle = i18n.get("collectionsTitle", language);
        String itemsTitle = i18n.get("itemsTitle", language);

        URICustomizer uriBuilder = uriCustomizer
                .clearParameters()
                .removePathSegment("items", -1);

        featureCollectionView.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(rootTitle, uriBuilder.copy()
                        .removeLastPathSegments(2 + subPathToLandingPage.size())
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
