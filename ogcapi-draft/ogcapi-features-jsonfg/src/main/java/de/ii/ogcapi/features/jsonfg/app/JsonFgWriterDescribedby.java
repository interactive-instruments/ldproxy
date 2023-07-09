/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.jsonfg.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ogcapi.features.geojson.domain.ModifiableStateGeoJson;
import de.ii.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableLink;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.xtraplatform.features.domain.FeatureTypeConfiguration;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class JsonFgWriterDescribedby implements GeoJsonWriter {

  private final I18n i18n;

  Map<String, Optional<Link>> collectionMap;

  @Inject
  public JsonFgWriterDescribedby(I18n i18n) {
    this.i18n = i18n;
  }

  @Override
  public JsonFgWriterDescribedby create() {
    return new JsonFgWriterDescribedby(i18n);
  }

  @Override
  public int getSortPriority() {
    // must be after the Links writer
    return 110;
  }

  @Override
  public void onStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    collectionMap = getCollectionMap(context.encoding());
    boolean isEnabled =
        collectionMap.keySet().stream()
            .anyMatch(collectionId -> isEnabled(context.encoding(), collectionId));

    if (isEnabled && context.encoding().isFeatureCollection()) {
      ModifiableStateGeoJson state = context.encoding().getState();

      List<Optional<Link>> links =
          collectionMap.values().stream().distinct().collect(Collectors.toUnmodifiableList());
      if (links.size() == 1 && links.get(0).isPresent()) {
        state.addCurrentFeatureCollectionLinks(links.get(0).get());
      }

      // add generic schemas
      state.addCurrentFeatureCollectionLinks(
          new ImmutableLink.Builder()
              .rel("describedby")
              .href("https://beta.schemas.opengis.net/json-fg/featurecollection.json")
              .type("application/schema+json")
              .title("This document is a JSON-FG FeatureCollection") // TODO add i18n
              .build(),
          new ImmutableLink.Builder()
              .rel("describedby")
              .href("https://geojson.org/schema/FeatureCollection.json")
              .type("application/schema+json")
              .title("This document is a GeoJSON FeatureCollection") // TODO add i18n
              .build());
    }

    // next chain for extensions
    next.accept(context);
  }

  @Override
  public void onFeatureStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    boolean isEnabled = isEnabled(context.encoding(), context.type());

    if (isEnabled && !context.encoding().isFeatureCollection()) {
      ModifiableStateGeoJson state = context.encoding().getState();

      Objects.requireNonNullElse(collectionMap.get(context.type()), Optional.<Link>empty())
          .ifPresent(link -> state.addCurrentFeatureLinks(link));

      // add generic schemas
      state.addCurrentFeatureLinks(
          new ImmutableLink.Builder()
              .rel("describedby")
              .href("https://beta.schemas.opengis.net/json-fg/feature.json")
              .type("application/schema+json")
              .title("This document is a JSON-FG Feature") // TODO add i18n
              .build(),
          new ImmutableLink.Builder()
              .rel("describedby")
              .href("https://geojson.org/schema/Feature.json")
              .type("application/schema+json")
              .title("This document is a GeoJSON Feature") // TODO add i18n
              .build());
    }

    // next chain for extensions
    next.accept(context);
  }

  private Map<String, Optional<Link>> getCollectionMap(
      FeatureTransformationContextGeoJson transformationContext) {
    ImmutableMap.Builder<String, Optional<Link>> builder = ImmutableMap.builder();
    boolean isFeatureCollection = transformationContext.isFeatureCollection();
    transformationContext
        .getFeatureSchemas()
        .keySet()
        .forEach(
            collectionId -> {
              if (isEnabled(transformationContext, collectionId)) {
                getSchemaUri(transformationContext, collectionId, isFeatureCollection)
                    .ifPresent(
                        schemaUri -> {
                          String label =
                              Optional.ofNullable(
                                      transformationContext
                                          .getApiData()
                                          .getCollections()
                                          .get(collectionId))
                                  .map(FeatureTypeConfiguration::getLabel)
                                  .orElse(collectionId);
                          Link link =
                              new ImmutableLink.Builder()
                                  .rel("describedby")
                                  .href(schemaUri)
                                  .type("application/schema+json")
                                  .title(
                                      i18n.get(
                                              isFeatureCollection
                                                  ? "schemaLinkCollection"
                                                  : "schemaLinkFeature",
                                              transformationContext.getLanguage())
                                          .replace("{{collection}}", label))
                                  .build();
                          builder.put(collectionId, Optional.of(link));
                        });
              } else {
                builder.put(collectionId, Optional.empty());
              }
            });

    return builder.build();
  }

  private Optional<String> getSchemaUri(
      FeatureTransformationContextGeoJson transformationContext,
      String collectionId,
      boolean isFeatureCollection) {
    return transformationContext
        .getApiData()
        .getExtension(JsonFgConfiguration.class, collectionId)
        .map(cfg -> isFeatureCollection ? cfg.getSchemaCollection() : cfg.getSchemaFeature());
  }

  private boolean isEnabled(
      FeatureTransformationContextGeoJson transformationContext, String collectionId) {
    return transformationContext
        .getApiData()
        .getExtension(JsonFgConfiguration.class, collectionId)
        .map(
            cfg ->
                cfg.isEnabled()
                    && Objects.requireNonNullElse(cfg.getDescribedby(), false)
                    && (cfg.getIncludeInGeoJson().contains(JsonFgConfiguration.OPTION.describedby)
                        || transformationContext
                            .getMediaType()
                            .equals(FeaturesFormatJsonFg.MEDIA_TYPE)
                        || transformationContext
                            .getMediaType()
                            .equals(FeaturesFormatJsonFgCompatibility.MEDIA_TYPE)))
        .orElse(false);
  }
}
