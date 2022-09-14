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
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ogcapi.features.json.fg.domain.JsonFgConfiguration;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class JsonFgWriterFeatureType implements GeoJsonWriter {

  static final String OPEN_TEMPLATE = "{{";
  public static String JSON_KEY = "featureType";

  boolean isEnabled;
  List<String> types;
  boolean templated;
  List<String> writeAtEnd;

  @Inject
  JsonFgWriterFeatureType() {}

  @Override
  public JsonFgWriterFeatureType create() {
    return new JsonFgWriterFeatureType();
  }

  @Override
  public int getSortPriority() {
    return 120;
  }

  @Override
  public void onStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    isEnabled = isEnabled(context.encoding());

    types =
        isEnabled
            ? context
                .encoding()
                .getApiData()
                .getExtension(JsonFgConfiguration.class, context.encoding().getCollectionId())
                .map(JsonFgConfiguration::getFeatureType)
                .orElse(ImmutableList.of())
            : ImmutableList.of();
    templated = types.stream().anyMatch(type -> type.contains(OPEN_TEMPLATE));
    writeAtEnd = ImmutableList.of();

    if (isEnabled && !templated && context.encoding().isFeatureCollection()) {
      writeTypes(context.encoding(), types);
    }

    // next chain for extensions
    next.accept(context);
  }

  @Override
  public void onFeatureStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (isEnabled) {
      if (templated) writeAtEnd = types;
      else if (!context.encoding().isFeatureCollection()) {
        writeTypes(context.encoding(), types);
      }
    }

    // next chain for extensions
    next.accept(context);
  }

  @Override
  public void onValue(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (!writeAtEnd.isEmpty()
        && context.schema().filter(FeatureSchema::isValue).isPresent()
        && Objects.nonNull(context.value())) {

      final FeatureSchema schema = context.schema().get();
      if (schema.isType()) {
        writeAtEnd =
            writeAtEnd.stream()
                .map(type -> type.replace("{{type}}", context.value()))
                .collect(Collectors.toUnmodifiableList());
      }
    }

    next.accept(context);
  }

  @Override
  public void onFeatureEnd(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (!writeAtEnd.isEmpty()) {
      writeTypes(context.encoding(), writeAtEnd);
      writeAtEnd = ImmutableList.of();
    }

    next.accept(context);
  }

  private boolean isEnabled(FeatureTransformationContextGeoJson transformationContext) {
    return transformationContext
        .getApiData()
        .getCollections()
        .get(transformationContext.getCollectionId())
        .getExtension(JsonFgConfiguration.class)
        .filter(JsonFgConfiguration::isEnabled)
        .filter(cfg -> Objects.nonNull(cfg.getFeatureType()) && !cfg.getFeatureType().isEmpty())
        .filter(
            cfg ->
                cfg.getIncludeInGeoJson().contains(JsonFgConfiguration.OPTION.featureType)
                    || transformationContext.getMediaType().equals(FeaturesFormatJsonFg.MEDIA_TYPE))
        .isPresent();
  }

  private void writeTypes(
      FeatureTransformationContextGeoJson transformationContext, List<String> types)
      throws IOException {
    if (types.stream().allMatch(type -> type.contains("{{type}}"))) return;

    JsonGenerator json = transformationContext.getJson();
    if (types.size() == 1) json.writeStringField(JSON_KEY, types.get(0));
    else {
      json.writeFieldName(JSON_KEY);
      json.writeStartArray(types.size());
      for (String t : types) if (!t.contains("{{type}}")) json.writeString(t);
      json.writeEndArray();
    }
  }
}
