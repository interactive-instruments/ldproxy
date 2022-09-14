/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.json.fg.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ogcapi.features.json.fg.domain.JsonFgConfiguration;
import de.ii.ogcapi.features.json.fg.domain.JsonFgGeometryType;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import java.io.IOException;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class JsonFgWriterPlace implements GeoJsonWriter {

  public static String JSON_KEY = "place";

  @Inject
  JsonFgWriterPlace() {}

  @Override
  public JsonFgWriterPlace create() {
    return new JsonFgWriterPlace();
  }

  boolean isEnabled;
  private boolean geometryOpen;
  private boolean additionalArray;
  private boolean hasPrimaryGeometry;
  private boolean suppressPlace;
  private TokenBuffer json;

  @Override
  public int getSortPriority() {
    return 140;
  }

  private void reset(EncodingAwareContextGeoJson context) {
    this.geometryOpen = false;
    this.hasPrimaryGeometry = false;
    this.additionalArray = false;
    this.json = new TokenBuffer(new ObjectMapper(), false);
    if (context.encoding().getPrettify()) {
      json.useDefaultPrettyPrinter();
    }
  }

  @Override
  public void onStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    isEnabled = isEnabled(context);

    // set 'place' to null, if the geometry is in WGS84 (in this case it is in "geometry")
    // and a simple feature geometry type
    suppressPlace =
        context.encoding().getTargetCrs().equals(context.encoding().getDefaultCrs())
            && context
                .encoding()
                .getFeatureSchema()
                .map(FeaturesFormatJsonFg::hasSimpleFeatureGeometryType)
                .orElse(true);

    next.accept(context);
  }

  @Override
  public void onFeatureStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (isEnabled) reset(context);

    next.accept(context);
  }

  @Override
  public void onObjectStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (isEnabled
        && !suppressPlace
        && context.schema().filter(SchemaBase::isSpatial).isPresent()
        && context.geometryType().isPresent()
        && context.schema().get().isPrimaryGeometry()) {

      String type =
          JsonFgGeometryType.forSimpleFeatureType(
                  context.geometryType().get(),
                  context
                      .schema()
                      .flatMap(s -> s.getConstraints().flatMap(SchemaConstraints::getComposite))
                      .orElse(false),
                  context
                      .schema()
                      .flatMap(s -> s.getConstraints().flatMap(SchemaConstraints::getClosed))
                      .orElse(false))
              .toString();

      json.writeFieldName(JSON_KEY);
      json.writeStartObject();
      json.writeStringField("type", type);
      json.writeFieldName("coordinates");

      if (type.equals("Polyhedron")) {
        json.writeStartArray();
        additionalArray = true;
      }

      geometryOpen = true;
      hasPrimaryGeometry = true;
    }

    next.accept(context);
  }

  @Override
  public void onArrayStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    if (geometryOpen) {
      json.writeStartArray();
    }

    next.accept(context);
  }

  @Override
  public void onArrayEnd(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    if (geometryOpen) {
      json.writeEndArray();
    }

    next.accept(context);
  }

  @Override
  public void onObjectEnd(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (context.schema().filter(SchemaBase::isSpatial).isPresent() && geometryOpen) {

      this.geometryOpen = false;

      if (additionalArray) {
        additionalArray = false;
        json.writeEndArray();
      }

      // close geometry object
      json.writeEndObject();
    }

    next.accept(context);
  }

  @Override
  public void onValue(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    if (geometryOpen) {
      json.writeRawValue(context.value());
    }

    next.accept(context);
  }

  @Override
  public void onFeatureEnd(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    if (isEnabled) {
      if (!hasPrimaryGeometry) {
        // write null geometry if none was written for this feature
        json.writeFieldName(JSON_KEY);
        json.writeNull();
      }
      json.serialize(context.encoding().getJson());
      json.flush();
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
                cfg.getIncludeInGeoJson().contains(JsonFgConfiguration.OPTION.place)
                    || context.encoding().getMediaType().equals(FeaturesFormatJsonFg.MEDIA_TYPE))
        .isPresent();
  }
}
