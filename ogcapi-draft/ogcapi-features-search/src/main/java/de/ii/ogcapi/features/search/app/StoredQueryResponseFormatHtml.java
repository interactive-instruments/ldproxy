/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ogcapi.features.html.domain.FeatureEncoderHtml;
import de.ii.ogcapi.features.html.domain.FeatureTransformationContextHtml;
import de.ii.ogcapi.features.html.domain.FeaturesFormatBaseHtml;
import de.ii.ogcapi.features.html.domain.FeaturesHtmlConfiguration;
import de.ii.ogcapi.features.html.domain.FeaturesHtmlConfiguration.POSITION;
import de.ii.ogcapi.features.html.domain.ImmutableFeatureTransformationContextHtml;
import de.ii.ogcapi.features.search.domain.SearchConfiguration;
import de.ii.ogcapi.foundation.domain.ApiMetadata;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.ogcapi.html.domain.NavigationDTO;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.services.domain.ServicesContext;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import de.ii.xtraplatform.web.domain.MustacheRenderer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class StoredQueryResponseFormatHtml extends FeaturesFormatBaseHtml {

  // TODO
  @Override
  public String getPathPattern() {
    return "^/?search(?:/"
        + "[\\w\\-]+" // TODO pattern
        + ")?/?$";
  }

  private final EntityRegistry entityRegistry;
  private final I18n i18n;
  private final URI servicesUri;
  private final MustacheRenderer mustacheRenderer;

  @Inject
  public StoredQueryResponseFormatHtml(
      EntityRegistry entityRegistry,
      MustacheRenderer mustacheRenderer,
      I18n i18n,
      ServicesContext servicesContext) {
    this.entityRegistry = entityRegistry;
    this.i18n = i18n;
    this.servicesUri = servicesContext.getUri();
    this.mustacheRenderer = mustacheRenderer;
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {

    // no additional operational checks for now, only validation; we can stop, if no validation is
    // requested
    if (apiValidation == MODE.NONE) return ValidationResult.of();

    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().mode(apiValidation);

    return builder.build();
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return SearchConfiguration.class;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
            .getExtension(getBuildingBlockConfigurationType())
            .map(ExtensionConfiguration::isEnabled)
            .orElse(false)
        && apiData
            .getExtension(HtmlConfiguration.class)
            .map(ExtensionConfiguration::isEnabled)
            .orElse(true);
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return apiData
            .getExtension(getBuildingBlockConfigurationType(), collectionId)
            .map(ExtensionConfiguration::isEnabled)
            .orElse(false)
        && apiData
            .getExtension(HtmlConfiguration.class, collectionId)
            .map(ExtensionConfiguration::isEnabled)
            .orElse(true);
  }

  @Override
  public Optional<FeatureTokenEncoder<?>> getFeatureEncoder(
      FeatureTransformationContext transformationContext, Optional<Locale> language) {
    OgcApi api = transformationContext.getApi();
    OgcApiDataV2 apiData = transformationContext.getApiData();
    String staticUrlPrefix = transformationContext.getOgcApiRequest().getStaticUrlPrefix();
    URICustomizer uriCustomizer = transformationContext.getOgcApiRequest().getUriCustomizer();
    StoredQueryResponseView featureTypeDataset;

    boolean bare =
        transformationContext.getOgcApiRequest().getUriCustomizer().getQueryParams().stream()
            .anyMatch(
                nameValuePair ->
                    nameValuePair.getName().equals("bare")
                        && nameValuePair.getValue().equals("true"));

    String queryId = transformationContext.getQueryId().orElseThrow();
    featureTypeDataset =
        createView(
            api,
            queryId,
            transformationContext.getQueryTitle().orElse(queryId),
            transformationContext.getQueryDescription().orElse(null),
            uriCustomizer.copy(),
            ImmutableMap.of(),
            staticUrlPrefix,
            bare,
            language,
            isNoIndexEnabledForApi(apiData),
            getMapPosition(apiData),
            ImmutableList.of());

    addNavigation(
        featureTypeDataset,
        apiData.getLabel(),
        transformationContext.getQueryTitle().orElse("Search"),
        transformationContext.getLinks(),
        uriCustomizer.copy(),
        language,
        apiData.getSubPath());

    FeatureTransformationContextHtml transformationContextHtml =
        ImmutableFeatureTransformationContextHtml.builder()
            .from(transformationContext)
            .collectionView(featureTypeDataset)
            .codelists(
                entityRegistry.getEntitiesForType(Codelist.class).stream()
                    .collect(Collectors.toMap(PersistentEntity::getId, c -> c)))
            .mustacheRenderer(mustacheRenderer)
            .i18n(i18n)
            .language(language)
            .build();

    return Optional.of(new FeatureEncoderHtml(transformationContextHtml));
  }

  private StoredQueryResponseView createView(
      OgcApi api,
      String id,
      String title,
      String description,
      URICustomizer uriCustomizer,
      Map<String, String> filterableFields,
      String staticUrlPrefix,
      boolean bare,
      Optional<Locale> language,
      boolean noIndex,
      POSITION mapPosition,
      List<String> geometryProperties) {
    OgcApiDataV2 apiData = api.getData();
    URI requestUri = null;
    try {
      requestUri = uriCustomizer.build();
    } catch (URISyntaxException e) {
      // ignore
    }

    Optional<HtmlConfiguration> htmlConfig = apiData.getExtension(HtmlConfiguration.class);
    String attribution = apiData.getMetadata().flatMap(ApiMetadata::getAttribution).orElse(null);

    Optional<FeaturesHtmlConfiguration> config =
        apiData.getExtension(FeaturesHtmlConfiguration.class);
    MapClient.Type mapClientType =
        config.map(FeaturesHtmlConfiguration::getMapClientType).orElse(MapClient.Type.MAP_LIBRE);
    String serviceUrl =
        new URICustomizer(servicesUri)
            .ensureLastPathSegments(apiData.getSubPath().toArray(String[]::new))
            .toString();
    String styleUrl =
        htmlConfig
            .map(
                cfg ->
                    cfg.getStyle(
                        config.map(FeaturesHtmlConfiguration::getStyle),
                        Optional.empty(),
                        serviceUrl))
            .orElse(null);
    boolean removeZoomLevelConstraints =
        config.map(FeaturesHtmlConfiguration::getRemoveZoomLevelConstraints).orElse(false);

    StoredQueryResponseView featureTypeDataset =
        new StoredQueryResponseView(
            api.getSpatialExtent(),
            bare ? "featureCollectionBare" : "featureCollection",
            requestUri,
            id,
            title,
            description,
            attribution,
            staticUrlPrefix,
            htmlConfig.orElse(null),
            null,
            noIndex,
            i18n,
            language,
            mapPosition,
            mapClientType,
            styleUrl,
            removeZoomLevelConstraints,
            geometryProperties);

    featureTypeDataset.temporalExtent = api.getTemporalExtent().orElse(null);
    api.getSpatialExtent()
        .ifPresent(
            bbox ->
                featureTypeDataset.bbox =
                    ImmutableMap.of(
                        "minLng",
                        Double.toString(bbox.getXmin()),
                        "minLat",
                        Double.toString(bbox.getYmin()),
                        "maxLng",
                        Double.toString(bbox.getXmax()),
                        "maxLat",
                        Double.toString(bbox.getYmax())));

    featureTypeDataset.uriBuilder =
        uriCustomizer.copy().ensureParameter("f", MEDIA_TYPE.parameter());
    featureTypeDataset.uriBuilderWithFOnly =
        uriCustomizer.copy().clearParameters().ensureParameter("f", MEDIA_TYPE.parameter());

    return featureTypeDataset;
  }

  private void addNavigation(
      StoredQueryResponseView featureCollectionView,
      String apiLabel,
      String queryLabel,
      List<Link> links,
      URICustomizer uriCustomizer,
      Optional<Locale> language,
      List<String> subPathToLandingPage) {

    String rootTitle = i18n.get("root", language);
    String searchTitle = i18n.get("storedQueriesTitle", language);

    URICustomizer resourceUri = uriCustomizer.copy().clearParameters();
    featureCollectionView.breadCrumbs =
        new ImmutableList.Builder<NavigationDTO>()
            .add(
                new NavigationDTO(
                    rootTitle,
                    resourceUri
                        .copy()
                        .removeLastPathSegments(subPathToLandingPage.size() + 2)
                        .toString()))
            .add(
                new NavigationDTO(
                    apiLabel, resourceUri.copy().removeLastPathSegments(2).toString()))
            .add(
                new NavigationDTO(
                    searchTitle, resourceUri.copy().removeLastPathSegments(1).toString()))
            .add(new NavigationDTO(queryLabel))
            .build();

    featureCollectionView.formats =
        links.stream()
            .filter(
                link ->
                    Objects.equals(link.getRel(), "alternate") && !link.getTypeLabel().isBlank())
            .sorted(Comparator.comparing(link -> link.getTypeLabel().toUpperCase()))
            .map(link -> new NavigationDTO(link.getTypeLabel(), link.getHref()))
            .collect(Collectors.toList());
  }
}
