/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.jsonfg.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.collections.schema.domain.SchemaConfiguration;
import de.ii.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableLink;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class JsonFgWriterDescribedby implements GeoJsonWriter {

  private final I18n i18n;
  boolean isEnabled;

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
    isEnabled = isEnabled(context.encoding());

    if (isEnabled && context.encoding().isFeatureCollection()) {
      String label =
          context
              .encoding()
              .getApiData()
              .getCollections()
              .get(context.encoding().getCollectionId())
              .getLabel();
      context
          .encoding()
          .getState()
          .addCurrentFeatureCollectionLinks(
              new ImmutableLink.Builder()
                  .rel("describedby")
                  .href(
                      context.encoding().getServiceUrl()
                          + "/collections/"
                          + context.encoding().getCollectionId()
                          + "/schemas/collection")
                  .type("application/schema+json")
                  .title(
                      i18n.get("schemaLinkCollection", context.encoding().getLanguage())
                          .replace("{{collection}}", label))
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
    if (isEnabled && !context.encoding().isFeatureCollection()) {
      String label =
          context
              .encoding()
              .getApiData()
              .getCollections()
              .get(context.encoding().getCollectionId())
              .getLabel();
      context
          .encoding()
          .getState()
          .addCurrentFeatureLinks(
              new ImmutableLink.Builder()
                  .rel("describedby")
                  .href(
                      context.encoding().getServiceUrl()
                          + "/collections/"
                          + context.encoding().getCollectionId()
                          + "/schemas/item")
                  .type("application/schema+json")
                  .title(
                      i18n.get("schemaLinkFeature", context.encoding().getLanguage())
                          .replace("{{collection}}", label))
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

  private boolean isEnabled(FeatureTransformationContextGeoJson transformationContext) {
    return transformationContext
            .getApiData()
            .getCollections()
            .get(transformationContext.getCollectionId())
            .getExtension(JsonFgConfiguration.class)
            .filter(JsonFgConfiguration::isEnabled)
            .filter(cfg -> Objects.requireNonNullElse(cfg.getDescribedby(), false))
            .filter(
                cfg ->
                    cfg.getIncludeInGeoJson().contains(JsonFgConfiguration.OPTION.describedby)
                        || transformationContext
                            .getMediaType()
                            .equals(FeaturesFormatJsonFg.MEDIA_TYPE)
                        || transformationContext
                            .getMediaType()
                            .equals(FeaturesFormatJsonFgCompatibility.MEDIA_TYPE))
            .isPresent()
        && transformationContext
            .getApiData()
            .getCollections()
            .get(transformationContext.getCollectionId())
            .getExtension(SchemaConfiguration.class)
            .filter(SchemaConfiguration::isEnabled)
            .isPresent();
  }
}
