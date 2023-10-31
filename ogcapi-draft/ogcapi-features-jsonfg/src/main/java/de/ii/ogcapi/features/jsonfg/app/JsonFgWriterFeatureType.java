/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.jsonfg.app;

import com.fasterxml.jackson.core.JsonGenerator;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.collections.schema.domain.SchemaConfiguration;
import de.ii.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.io.IOException;
import java.util.HashMap;
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
public class JsonFgWriterFeatureType implements GeoJsonWriter {

  static final String OPEN_TEMPLATE = "{{";
  public static String JSON_KEY = "featureType";
  public static String JSON_KEY_SCHEMA = "featureSchema";

  Map<String, List<String>> collectionMap;
  boolean isEnabled;
  boolean homogenous;
  List<String> writeAtEnd;
  Map<String, Optional<String>> schemaMap;
  Map<String, String> effectiveSchemas;

  @Inject
  JsonFgWriterFeatureType() {}

  @Override
  public JsonFgWriterFeatureType create() {
    return new JsonFgWriterFeatureType();
  }

  @Override
  public int getSortPriority() {
    return 24;
  }

  @Override
  public void onStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    collectionMap = getCollectionMap(context.encoding());
    isEnabled = collectionMap.values().stream().anyMatch(types -> !types.isEmpty());
    homogenous =
        collectionMap.values().stream()
                .noneMatch(types -> types.stream().anyMatch(type -> type.contains(OPEN_TEMPLATE)))
            && context.encoding().isFeatureCollection()
            && collectionMap.size() == 1;
    writeAtEnd = ImmutableList.of();
    schemaMap = getSchemaMap(context.encoding());
    effectiveSchemas = new HashMap<>();

    if (isEnabled && homogenous) {
      writeTypes(context.encoding(), collectionMap.values().iterator().next());
      writeSingleSchema(context.encoding(), schemaMap.values().iterator().next());
    }

    // next chain for extensions
    next.accept(context);
  }

  @Override
  public void onFeatureStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (isEnabled && !homogenous) {
      List<String> types = collectionMap.get(context.type());
      if (Objects.nonNull(types) && !types.isEmpty()) {
        if (types.stream().anyMatch(type -> type.contains(OPEN_TEMPLATE))) {
          writeAtEnd = types;
        } else {
          writeTypes(context.encoding(), types);
          if (schemaMap.containsKey(context.type())) {
            schemaMap
                .get(context.type())
                .ifPresent(s -> types.forEach(t -> effectiveSchemas.put(t, s)));
          }
        }
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
        if (schemaMap.containsKey(context.type())) {
          schemaMap
              .get(context.type())
              .ifPresent(
                  s -> {
                    writeAtEnd.stream()
                        .map(type -> type.replace("{{type}}", context.value()))
                        .forEach(type -> effectiveSchemas.put(type, s));
                  });
        }
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
    if (!context.encoding().isFeatureCollection()) {
      writeSchemas(context.encoding());
    }

    next.accept(context);
  }

  @Override
  public void onEnd(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (context.encoding().isFeatureCollection()) {
      writeSchemas(context.encoding());
    }

    next.accept(context);
  }

  private Map<String, List<String>> getCollectionMap(
      FeatureTransformationContextGeoJson transformationContext) {
    ImmutableMap.Builder<String, List<String>> builder = ImmutableMap.builder();
    transformationContext
        .getFeatureSchemas()
        .keySet()
        .forEach(
            collectionId ->
                transformationContext
                    .getApiData()
                    .getExtension(JsonFgConfiguration.class, collectionId)
                    .ifPresentOrElse(
                        cfg -> {
                          if (cfg.isEnabled()
                              && (cfg.getIncludeInGeoJson()
                                      .contains(JsonFgConfiguration.OPTION.featureType)
                                  || transformationContext
                                      .getMediaType()
                                      .equals(FeaturesFormatJsonFg.MEDIA_TYPE)
                                  || transformationContext
                                      .getMediaType()
                                      .equals(FeaturesFormatJsonFgCompatibility.MEDIA_TYPE))) {
                            List<String> value = cfg.getFeatureType();
                            if (Objects.isNull(value) || value.isEmpty()) {
                              value =
                                  transformationContext
                                      .getFeatureSchemas()
                                      .get(collectionId)
                                      .flatMap(FeatureSchema::getObjectType)
                                      .map(ImmutableList::of)
                                      .orElse(ImmutableList.of());
                            }
                            builder.put(collectionId, value);
                          } else {
                            builder.put(collectionId, ImmutableList.of());
                          }
                        },
                        () -> builder.put(collectionId, ImmutableList.of())));
    return builder.build();
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

  private Map<String, Optional<String>> getSchemaMap(
      FeatureTransformationContextGeoJson transformationContext) {
    ImmutableMap.Builder<String, Optional<String>> builder = ImmutableMap.builder();
    transformationContext
        .getFeatureSchemas()
        .keySet()
        .forEach(
            collectionId ->
                transformationContext
                    .getApiData()
                    .getExtension(JsonFgConfiguration.class, collectionId)
                    .ifPresentOrElse(
                        cfg -> {
                          if (cfg.isEnabled()
                              && !Objects.requireNonNullElse(
                                      cfg.getFeatureType(), ImmutableList.of())
                                  .isEmpty()
                              && (cfg.getIncludeInGeoJson()
                                      .contains(JsonFgConfiguration.OPTION.featureType)
                                  || transformationContext
                                      .getMediaType()
                                      .equals(FeaturesFormatJsonFg.MEDIA_TYPE)
                                  || transformationContext
                                      .getMediaType()
                                      .equals(FeaturesFormatJsonFgCompatibility.MEDIA_TYPE))) {
                            Optional<String> value =
                                transformationContext
                                    .getApiData()
                                    .getExtension(SchemaConfiguration.class, collectionId)
                                    .filter(ExtensionConfiguration::isEnabled)
                                    .map(
                                        ignore ->
                                            String.format(
                                                "%s/collections/%s/schema",
                                                transformationContext.getServiceUrl(),
                                                collectionId));
                            builder.put(collectionId, value);
                          } else {
                            builder.put(collectionId, Optional.empty());
                          }
                        },
                        () -> builder.put(collectionId, Optional.empty())));
    return builder.build();
  }

  private void writeSingleSchema(
      FeatureTransformationContextGeoJson transformationContext, Optional<String> schema) {
    schema.ifPresent(
        s -> {
          try {
            transformationContext.getJson().writeStringField(JSON_KEY_SCHEMA, s);
          } catch (IOException ignore) {
          }
        });
  }

  private void writeSchemas(FeatureTransformationContextGeoJson transformationContext)
      throws IOException {
    if (effectiveSchemas.size() == 1) {
      transformationContext
          .getJson()
          .writeStringField(JSON_KEY_SCHEMA, effectiveSchemas.values().iterator().next());
    } else if (!effectiveSchemas.isEmpty()) {
      transformationContext.getJson().writeObjectField(JSON_KEY_SCHEMA, effectiveSchemas);
    }
  }
}
