/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.CollectionExtension;
import de.ii.ogcapi.collections.domain.CollectionsConfiguration;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiCollection;
import de.ii.ogcapi.common.domain.ImmutableLandingPage;
import de.ii.ogcapi.common.domain.ImmutableLandingPage.Builder;
import de.ii.ogcapi.common.domain.LandingPageExtension;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableLink;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.xtraplatform.features.domain.FeatureTypeConfiguration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class CollectionsOnLandingPage implements LandingPageExtension {

  private final I18n i18n;
  private final ExtensionRegistry extensionRegistry;

  @Inject
  public CollectionsOnLandingPage(ExtensionRegistry extensionRegistry, I18n i18n) {
    this.extensionRegistry = extensionRegistry;
    this.i18n = i18n;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return CollectionsConfiguration.class;
  }

  @Override
  public ImmutableLandingPage.Builder process(
      Builder landingPageBuilder,
      OgcApi api,
      URICustomizer uriCustomizer,
      ApiMediaType mediaType,
      List<ApiMediaType> alternateMediaTypes,
      Optional<Locale> language) {
    OgcApiDataV2 apiData = api.getData();
    if (!isEnabledForApi(apiData)) {
      return landingPageBuilder;
    }

    List<String> collectionNames =
        apiData.getCollections().values().stream()
            .filter(FeatureTypeConfigurationOgcApi::getEnabled)
            .map(FeatureTypeConfiguration::getLabel)
            .collect(Collectors.toList());
    String suffix =
        !collectionNames.isEmpty() && collectionNames.size() <= 4
            ? String.format(" (%s)", String.join(", ", collectionNames))
            : "";
    addDataLinks(landingPageBuilder, uriCustomizer, language, suffix);

    ImmutableList.Builder<Link> distributionLinks =
        new ImmutableList.Builder<Link>()
            .addAll(
                apiData
                    .getExtension(CollectionsConfiguration.class)
                    .map(CollectionsConfiguration::getAdditionalLinks)
                    .orElse(ImmutableList.of())
                    .stream()
                    .filter(link -> Objects.equals(link.getRel(), "enclosure"))
                    .collect(Collectors.toUnmodifiableList()));

    // for cases with a single collection, that collection is not reported as a sub-dataset and we
    // need to determine the distribution links (enclosure links provided in additonalLinks and the
    // regular items links to the features in the API)
    if (apiData.getCollections().size() == 1) {
      String collectionId = apiData.getCollections().keySet().iterator().next();
      FeatureTypeConfigurationOgcApi featureTypeConfiguration =
          apiData.getCollections().get(collectionId);
      //noinspection ConstantConditions
      distributionLinks.addAll(
          featureTypeConfiguration.getAdditionalLinks().stream()
              .filter(link -> Objects.equals(link.getRel(), "enclosure"))
              .collect(Collectors.toUnmodifiableList()));

      ImmutableOgcApiCollection.Builder ogcApiCollection =
          ImmutableOgcApiCollection.builder().id(collectionId);
      for (CollectionExtension ogcApiCollectionExtension :
          extensionRegistry.getExtensionsForType(CollectionExtension.class)) {
        ogcApiCollection =
            ogcApiCollectionExtension.process(
                ogcApiCollection,
                featureTypeConfiguration,
                api,
                uriCustomizer
                    .copy()
                    .clearParameters()
                    .ensureLastPathSegments("collections", collectionId)
                    .ensureNoTrailingSlash(),
                false,
                mediaType,
                alternateMediaTypes,
                language);
      }
      distributionLinks.addAll(
          ogcApiCollection.build().getLinks().stream()
              .filter(
                  link ->
                      Objects.equals(link.getRel(), "items")
                          && !Objects.equals(link.getType(), "text/html"))
              .collect(Collectors.toUnmodifiableList()));
    }

    landingPageBuilder.putExtensions("datasetDownloadLinks", distributionLinks.build());

    return landingPageBuilder;
  }

  private void addDataLinks(
      Builder landingPageBuilder,
      URICustomizer uriCustomizer,
      Optional<Locale> language,
      String suffix) {
    landingPageBuilder
        .addLinks(
            new ImmutableLink.Builder()
                .href(
                    uriCustomizer
                        .copy()
                        .ensureNoTrailingSlash()
                        .ensureLastPathSegment("collections")
                        .removeParameters("f")
                        .toString())
                .rel("data")
                .title(i18n.get("dataLink", language) + suffix)
                .build())
        .addLinks(
            new ImmutableLink.Builder()
                .href(
                    uriCustomizer
                        .copy()
                        .ensureNoTrailingSlash()
                        .ensureLastPathSegment("collections")
                        .removeParameters("f")
                        .toString())
                .rel("http://www.opengis.net/def/rel/ogc/1.0/data")
                .title(i18n.get("dataLink", language) + suffix)
                .build());
  }
}
