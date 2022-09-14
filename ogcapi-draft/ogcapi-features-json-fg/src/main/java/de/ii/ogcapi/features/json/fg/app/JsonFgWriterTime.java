/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.json.fg.app;

import com.fasterxml.jackson.core.JsonGenerator;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ogcapi.features.json.fg.domain.JsonFgConfiguration;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class JsonFgWriterTime implements GeoJsonWriter {

  public static String JSON_KEY = "time";

  boolean isEnabled;
  String currentIntervalStart;
  String currentIntervalEnd;
  String currentInstant;

  @Inject
  JsonFgWriterTime() {}

  @Override
  public JsonFgWriterTime create() {
    return new JsonFgWriterTime();
  }

  @Override
  public int getSortPriority() {
    return 100;
  }

  @Override
  public void onStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    isEnabled = isEnabled(context);

    // next chain for extensions
    next.accept(context);
  }

  @Override
  public void onFeatureStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) {
    if (isEnabled) {
      currentIntervalStart = null;
      currentIntervalEnd = null;
      currentInstant = null;
    }

    // next chain for extensions
    next.accept(context);
  }

  @Override
  public void onFeatureEnd(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    next.accept(context);

    if (isEnabled) {
      JsonGenerator json = context.encoding().getJson();
      if (Objects.nonNull(currentInstant)) {
        json.writeFieldName(JSON_KEY);
        json.writeStartObject();
        json.writeStringField("instant", currentInstant);
        json.writeEndObject();
      } else if (Objects.nonNull(currentIntervalStart) || Objects.nonNull(currentIntervalEnd)) {
        json.writeFieldName(JSON_KEY);
        json.writeStartObject();
        json.writeArrayFieldStart("interval");
        if (Objects.nonNull(currentIntervalStart)) json.writeString(currentIntervalStart);
        else json.writeNull();
        if (Objects.nonNull(currentIntervalEnd)) json.writeString(currentIntervalEnd);
        else json.writeNull();
        json.writeEndArray();
        json.writeEndObject();
      } else {
        json.writeFieldName(JSON_KEY);
        json.writeNull();
      }
    }
  }

  @Override
  public void onValue(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (isEnabled
        && context.schema().filter(FeatureSchema::isValue).isPresent()
        && Objects.nonNull(context.value())) {
      final FeatureSchema schema = context.schema().get();
      if (schema.isPrimaryInstant()) currentInstant = context.value();
      else if (schema.isPrimaryIntervalStart()) currentIntervalStart = context.value();
      else if (schema.isPrimaryIntervalEnd()) currentIntervalEnd = context.value();
    }

    next.accept(context);
  }

  private boolean isEnabled(EncodingAwareContextGeoJson context) {
    return context
        .encoding()
        .getApiData()
        .getCollections()
        .get(context.encoding().getCollectionId())
        .getExtension(JsonFgConfiguration.class)
        .filter(JsonFgConfiguration::isEnabled)
        .filter(
            cfg ->
                cfg.getIncludeInGeoJson().contains(JsonFgConfiguration.OPTION.time)
                    || context.encoding().getMediaType().equals(FeaturesFormatJsonFg.MEDIA_TYPE))
        .isPresent();
  }
}
