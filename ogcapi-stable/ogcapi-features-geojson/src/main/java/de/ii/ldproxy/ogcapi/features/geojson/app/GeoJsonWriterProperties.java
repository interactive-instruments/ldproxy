/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import com.fasterxml.jackson.core.JsonGenerator;
import de.ii.ldproxy.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class GeoJsonWriterProperties implements GeoJsonWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonWriterProperties.class);

  private boolean currentStarted;

  public GeoJsonWriterProperties() {
  }

  @Override
  public GeoJsonWriterProperties create() {
    return new GeoJsonWriterProperties();
  }

  @Override
  public int getSortPriority() {
    return 40;
  }

  @Override
  public void onPropertiesEnd(EncodingAwareContextGeoJson context,
      Consumer<EncodingAwareContextGeoJson> next) throws IOException {

    if (currentStarted) {
      this.currentStarted = false;

      // end of "properties"
      context.encoding().getJson()
          .writeEndObject();
    }

    next.accept(context);
  }

  @Override
  public void onArrayStart(EncodingAwareContextGeoJson context,
      Consumer<EncodingAwareContextGeoJson> next) throws IOException {
    if (context.schema()
        .filter(FeatureSchema::isArray)
        .isPresent()) {
      FeatureSchema schema = context.schema().get();

      if (!currentStarted) {
        this.currentStarted = true;

        context.encoding().getJson()
            .writeObjectFieldStart(getPropertiesFieldName());
      }

      context.encoding().getJson().writeArrayFieldStart(schema.getName());
    }

    next.accept(context);
  }

  @Override
  public void onObjectStart(EncodingAwareContextGeoJson context,
      Consumer<EncodingAwareContextGeoJson> next) throws IOException {
    if (context.schema()
        .filter(FeatureSchema::isObject)
        .isPresent()) {
      FeatureSchema schema = context.schema().get();

      if (!currentStarted) {
        this.currentStarted = true;

        context.encoding().getJson()
            .writeObjectFieldStart(getPropertiesFieldName());
      }

      openObject(context.encoding(), schema);
    }

    next.accept(context);
  }

  @Override
  public void onObjectEnd(EncodingAwareContextGeoJson context,
      Consumer<EncodingAwareContextGeoJson> next) throws IOException {
    if (context.schema()
        .filter(FeatureSchema::isObject)
        .isPresent()) {
      closeObject(context.encoding());
    }

    next.accept(context);
  }

  @Override
  public void onArrayEnd(EncodingAwareContextGeoJson context,
      Consumer<EncodingAwareContextGeoJson> next) throws IOException {
    if (context.schema()
        .filter(FeatureSchema::isArray)
        .isPresent()) {
      context.encoding().getJson().writeEndArray();
    }

    next.accept(context);
  }

  @Override
  public void onValue(EncodingAwareContextGeoJson context,
      Consumer<EncodingAwareContextGeoJson> next) throws IOException {
    if (!shouldSkipProperty(context)) {
      FeatureSchema schema = context.schema().get();
      String value = context.value();
      JsonGenerator json = context.encoding().getJson();

      if (!currentStarted) {
        this.currentStarted = true;

        context.encoding().getJson()
            .writeObjectFieldStart(getPropertiesFieldName());
      }

      if (schema.isArray()) {
        writeValue(json, value, schema.getValueType().orElse(Type.STRING));
      } else {
        json.writeFieldName(schema.getName());
        writeValue(json, value, schema.getType());
      }
    }

    next.accept(context);
  }

  protected String getPropertiesFieldName() {
    return "properties";
  }

  protected boolean shouldSkipProperty(EncodingAwareContextGeoJson context) {
    return !hasMappingAndValue(context)
        || (context.schema().get().isId()
        || context.inGeometry());
  }

  protected boolean hasMappingAndValue(EncodingAwareContextGeoJson context) {
    return context.schema()
        .filter(FeatureSchema::isValue)
        .isPresent()
        && Objects.nonNull(context.value());
  }

  //TODO: centralize value type mappings (either as transformer or as part of context)
  private void writeValue(JsonGenerator json, String value, Type type) throws IOException {
    switch (type) {

      case BOOLEAN:
        //TODO: normalize in decoder
        json.writeBoolean(
            value.equalsIgnoreCase("t") || value.equalsIgnoreCase("true") || value.equals("1"));
        break;
      case INTEGER:
        try {
          json.writeNumber(Long.parseLong(value));
          break;
        } catch (NumberFormatException e) {
          //ignore
        }
      case FLOAT:
        try {
          json.writeNumber(Double.parseDouble(value));
          break;
        } catch (NumberFormatException e) {
          //ignore
        }
      default:
        json.writeString(value);
    }
  }

  private void openObject(FeatureTransformationContextGeoJson encoding, FeatureSchema schema) throws IOException {
      if (schema.isArray()) {
          encoding.getJson().writeStartObject();
      } else {
        encoding.getJson().writeObjectFieldStart(schema.getName());
      }
  }

  private void closeObject(FeatureTransformationContextGeoJson encoding) throws IOException {
      encoding.getJson().writeEndObject();
  }

}
