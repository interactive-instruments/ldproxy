/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.collections.domain.CollectionExtension;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiCollection.Builder;
import de.ii.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ogcapi.common.domain.OgcApiExtent;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableLink;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.TemporalExtent;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class CollectionExtensionFeatures implements CollectionExtension {

  private final I18n i18n;
  private final ExtensionRegistry extensionRegistry;

  @Inject
  public CollectionExtensionFeatures(ExtensionRegistry extensionRegistry, I18n i18n) {
    this.extensionRegistry = extensionRegistry;
    this.i18n = i18n;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return FeaturesCoreConfiguration.class;
  }

  @Override
  public ImmutableOgcApiCollection.Builder process(
      Builder collection,
      FeatureTypeConfigurationOgcApi featureType,
      OgcApi api,
      URICustomizer uriCustomizer,
      boolean isNested,
      ApiMediaType mediaType,
      List<ApiMediaType> alternateMediaTypes,
      Optional<Locale> language) {

    init(collection, featureType, api);

    URICustomizer uriBuilder = uriCustomizer.copy().ensureNoTrailingSlash().clearParameters();

    addSelfLink(collection, featureType, isNested, language, uriBuilder);

    addItemsLinks(collection, featureType, api, language, uriBuilder);

    addDescribeFeatureTypeUrl(collection, language, Optional.empty());

    addExtents(collection, featureType, api);

    return collection;
  }

  private void addSelfLink(
      Builder collection,
      FeatureTypeConfigurationOgcApi featureType,
      boolean isNested,
      Optional<Locale> language,
      URICustomizer uriBuilder) {
    if (isNested) {
      // also add an untyped a self link in the Collections resource, otherwise the standard links
      // are already there
      collection.addLinks(
          new ImmutableLink.Builder()
              .href(
                  uriBuilder
                      .copy()
                      .ensureLastPathSegments("collections", featureType.getId())
                      .removeParameters("f")
                      .toString())
              .rel("self")
              .title(
                  i18n.get("selfLinkCollection", language)
                      .replace("{{collection}}", featureType.getLabel()))
              .build());
    }
  }

  private void addExtents(
      Builder collection, FeatureTypeConfigurationOgcApi featureType, OgcApi api) {
    // only add extents for cases where we can filter using spatial / temporal predicates
    Optional<FeaturesCollectionQueryables> queryables =
        featureType
            .getExtension(FeaturesCoreConfiguration.class)
            .flatMap(FeaturesCoreConfiguration::getQueryables);
    boolean hasSpatialQueryable =
        queryables
            .map(FeaturesCollectionQueryables::getSpatial)
            .filter(spatial -> !spatial.isEmpty())
            .isPresent();
    boolean hasTemporalQueryable =
        queryables
            .map(FeaturesCollectionQueryables::getTemporal)
            .filter(temporal -> !temporal.isEmpty())
            .isPresent();
    Optional<BoundingBox> spatial = api.getSpatialExtent(featureType.getId());
    Optional<TemporalExtent> temporal = api.getTemporalExtent(featureType.getId());
    if (hasSpatialQueryable && hasTemporalQueryable) {
      collection.extent(OgcApiExtent.of(spatial, temporal));
    } else if (hasSpatialQueryable) {
      collection.extent(OgcApiExtent.of(spatial, Optional.empty()));
    } else if (hasTemporalQueryable) {
      collection.extent(OgcApiExtent.of(Optional.empty(), temporal));
    }
  }

  private void addDescribeFeatureTypeUrl(
      Builder collection, Optional<Locale> language, Optional<String> describeFeatureTypeUrl) {
    describeFeatureTypeUrl.ifPresent(
        s ->
            collection.addLinks(
                new ImmutableLink.Builder()
                    .href(s)
                    .rel("describedby")
                    .type("application/xml")
                    .title(i18n.get("describedByXsdLink", language))
                    .build()));
  }

  private void addItemsLinks(
      Builder collection,
      FeatureTypeConfigurationOgcApi featureType,
      OgcApi api,
      Optional<Locale> language,
      URICustomizer uriBuilder) {
    List<ApiMediaType> featureMediaTypes =
        extensionRegistry.getExtensionsForType(FeatureFormatExtension.class).stream()
            .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(api.getData()))
            .map(FormatExtension::getMediaType)
            .collect(Collectors.toList());

    featureMediaTypes.forEach(
        mtype ->
            collection.addLinks(
                new ImmutableLink.Builder()
                    .href(
                        uriBuilder
                            .ensureLastPathSegments("collections", featureType.getId(), "items")
                            .setParameter("f", mtype.parameter())
                            .toString())
                    .rel("items")
                    .type(mtype.type().toString())
                    .title(
                        i18n.get("itemsLink", language)
                            .replace("{{collection}}", featureType.getLabel())
                            .replace("{{type}}", mtype.label()))
                    .build()));
  }

  private void init(Builder collection, FeatureTypeConfigurationOgcApi featureType, OgcApi api) {
    collection
        .title(featureType.getLabel())
        .description(featureType.getDescription())
        .itemType(
            featureType
                .getExtension(FeaturesCoreConfiguration.class)
                .filter(ExtensionConfiguration::isEnabled)
                .flatMap(FeaturesCoreConfiguration::getItemType)
                .map(Enum::toString)
                .orElse(FeaturesCoreConfiguration.ItemType.UNKNOWN.toString()));

    api.getItemCount(featureType.getId())
        .filter(count -> count >= 0)
        .ifPresent(count -> collection.putExtensions("itemCount", count));
  }

  public static OgcApiCollection createNestedCollection(
      FeatureTypeConfigurationOgcApi featureType,
      OgcApi api,
      ApiMediaType mediaType,
      List<ApiMediaType> alternateMediaTypes,
      Optional<Locale> language,
      URICustomizer uriCustomizer,
      List<CollectionExtension> collectionExtenders) {
    ImmutableOgcApiCollection.Builder ogcApiCollection =
        ImmutableOgcApiCollection.builder().id(featureType.getId());

    for (CollectionExtension ogcApiCollectionExtension : collectionExtenders) {
      ogcApiCollection =
          ogcApiCollectionExtension.process(
              ogcApiCollection,
              featureType,
              api,
              uriCustomizer.copy(),
              true,
              mediaType,
              alternateMediaTypes,
              language);
    }

    return ogcApiCollection.build();
  }
}
