/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.app;

import com.fasterxml.jackson.core.JsonGenerator;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import java.io.IOException;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class GeoJsonWriterProperties implements GeoJsonWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonWriterProperties.class);

  private boolean currentStarted;
  private int embeddedFeatureNestingLevel;
  private Stack<Boolean> currentEmbeddedStarted;

  @Inject
  public GeoJsonWriterProperties() {}

  @Override
  public GeoJsonWriterProperties create() {
    return new GeoJsonWriterProperties();
  }

  @Override
  public int getSortPriority() {
    return 40;
  }

  @Override
  public void onFeatureStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    this.currentStarted = false;
    this.embeddedFeatureNestingLevel = 0;
    this.currentEmbeddedStarted = new Stack<>();

    next.accept(context);
  }

  @Override
  public void onPropertiesEnd(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    if (currentStarted) {
      this.currentStarted = false;

      // end of "properties"
      context.encoding().getJson().writeEndObject();
    } else {

      // no properties, write null member
      context.encoding().getJson().writeNullField(getPropertiesFieldName());
    }

    next.accept(context);
  }

  @Override
  public void onArrayStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (context.schema().filter(FeatureSchema::isArray).isPresent()) {
      FeatureSchema schema = context.schema().get();

      if (!currentStarted) {
        this.currentStarted = true;

        context.encoding().getJson().writeObjectFieldStart(getPropertiesFieldName());
      }

      if (embeddedFeatureNestingLevel > 0 && !currentEmbeddedStarted.peek()) {
        this.currentEmbeddedStarted.set(currentEmbeddedStarted.size() - 1, true);

        context.encoding().getJson().writeObjectFieldStart(getPropertiesFieldName());
      }

      context.encoding().getJson().writeArrayFieldStart(schema.getName());
    }

    next.accept(context);
  }

  @Override
  public void onObjectStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (context.schema().filter(FeatureSchema::isObject).isPresent()) {
      FeatureSchema schema = context.schema().get();

      if (!currentStarted) {
        this.currentStarted = true;

        context.encoding().getJson().writeObjectFieldStart(getPropertiesFieldName());
      }

      if (embeddedFeatureNestingLevel > 0 && !currentEmbeddedStarted.peek()) {
        this.currentEmbeddedStarted.set(currentEmbeddedStarted.size() - 1, true);

        context.encoding().getJson().writeObjectFieldStart(getPropertiesFieldName());
      }

      openObject(context.encoding(), schema);
    }

    next.accept(context);
  }

  @Override
  public void onObjectEnd(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (context.schema().filter(FeatureSchema::isObject).isPresent()) {
      FeatureSchema schema = context.schema().get();

      closeObject(context.encoding(), schema);
    }

    next.accept(context);
  }

  @Override
  public void onArrayEnd(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (context.schema().filter(FeatureSchema::isArray).isPresent()) {
      context.encoding().getJson().writeEndArray();
    }

    next.accept(context);
  }

  @Override
  public void onValue(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (!shouldSkipProperty(context)) {
      FeatureSchema schema = context.schema().get();
      String value = context.value();
      JsonGenerator json = context.encoding().getJson();

      if (!currentStarted) {
        this.currentStarted = true;

        context.encoding().getJson().writeObjectFieldStart(getPropertiesFieldName());
      }

      if (embeddedFeatureNestingLevel > 0 && !currentEmbeddedStarted.peek()) {
        this.currentEmbeddedStarted.set(currentEmbeddedStarted.size() - 1, true);

        context.encoding().getJson().writeObjectFieldStart(getPropertiesFieldName());
      }

      if (schema.isArray() && !context.encoding().getGeoJsonConfig().isFlattened()) {
        writeValue(json, value, getValueType(schema, context.valueType()));
      } else {
        json.writeFieldName(schema.getName());
        Type valueType =
            schema.getCoalesce().isEmpty()
                    || (schema.getType() != Type.VALUE && schema.getType() != Type.FEATURE_REF)
                ? schema.getType()
                : getValueType(schema, context.valueType());
        writeValue(json, value, valueType);
      }
    }

    next.accept(context);
  }

  private Type getValueType(FeatureSchema schema, Type fromValue) {
    return schema
        .getValueType()
        .filter(t -> t != Type.VALUE && t != Type.VALUE_ARRAY)
        .orElse(Objects.requireNonNullElse(fromValue, Type.STRING));
  }

  protected String getPropertiesFieldName() {
    return "properties";
  }

  protected boolean shouldSkipProperty(EncodingAwareContextGeoJson context) {
    return !hasMapping(context)
        || (context.schema().get().isId()
            || context.schema().get().isEmbeddedId()
            || context.inGeometry());
  }

  protected boolean hasMapping(EncodingAwareContextGeoJson context) {
    return context.schema().filter(FeatureSchema::isValue).isPresent();
  }

  // TODO: centralize value type mappings (either as transformer or as part of context)
  private void writeValue(JsonGenerator json, String value, Type type) throws IOException {
    if (Objects.isNull(value)) {
      json.writeNull();
      return;
    }

    switch (type) {
      case BOOLEAN:
        // TODO: normalize in decoder
        json.writeBoolean(
            value.equalsIgnoreCase("t") || value.equalsIgnoreCase("true") || value.equals("1"));
        break;
      case INTEGER:
        try {
          json.writeNumber(Long.parseLong(value));
          break;
        } catch (NumberFormatException e) {
          // ignore
        }
      case FLOAT:
        try {
          json.writeNumber(Double.parseDouble(value));
          break;
        } catch (NumberFormatException e) {
          // ignore
        }
      default:
        json.writeString(value);
    }
  }

  private void openObject(FeatureTransformationContextGeoJson encoding, FeatureSchema schema)
      throws IOException {
    if (schema.isArray()) {
      encoding.getJson().writeStartObject();
    } else {
      encoding.getJson().writeObjectFieldStart(schema.getName());
    }

    if (schema.getRole().filter(r -> r == Role.EMBEDDED_FEATURE).isPresent()) {
      encoding.getJson().writeStringField("type", "Feature");
      this.embeddedFeatureNestingLevel++;
      this.currentEmbeddedStarted.push(false);
    }
  }

  private void closeObject(FeatureTransformationContextGeoJson encoding, FeatureSchema schema)
      throws IOException {
    if (schema.getRole().filter(r -> r == Role.EMBEDDED_FEATURE).isPresent()) {

      if (currentEmbeddedStarted.peek()) {

        // end of "properties"
        encoding.getJson().writeEndObject();
      } else {

        // no properties, write null member
        encoding.getJson().writeNullField(getPropertiesFieldName());
      }

      this.embeddedFeatureNestingLevel--;
      this.currentEmbeddedStarted.pop();
    }

    encoding.getJson().writeEndObject();
  }
}
