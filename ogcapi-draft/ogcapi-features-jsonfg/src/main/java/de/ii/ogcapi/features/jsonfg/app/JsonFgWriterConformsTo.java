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
import de.ii.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class JsonFgWriterConformsTo implements GeoJsonWriter {

  public static String JSON_KEY = "conformsTo";

  public static String CURIE_CORE = "[ogc-json-fg-1-0.2:core]";
  public static String CURIE_3D = "[ogc-json-fg-1-0.2:3d]";
  public static String CURIE_TYPE = "[ogc-json-fg-1-0.2:types-schemas]";
  public static String URI_CORE = "http://www.opengis.net/spec/json-fg-1/0.2/conf/core";
  public static String URI_3D = "http://www.opengis.net/spec/json-fg-1/0.2/conf/3d";
  public static String URI_TYPE = "http://www.opengis.net/spec/json-fg-1/0.2/conf/types-schemas";

  Map<String, Boolean> collectionMap;
  boolean isEnabled;
  boolean has3d;
  boolean hasFeatureType;
  boolean useCuries;

  @Inject
  JsonFgWriterConformsTo() {}

  @Override
  public JsonFgWriterConformsTo create() {
    return new JsonFgWriterConformsTo();
  }

  @Override
  public int getSortPriority() {
    return 135;
  }

  @Override
  public void onStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    isEnabled = isEnabled(context.encoding());
    has3d = has3d(context.encoding());
    hasFeatureType = hasFeatureType(context.encoding());
    useCuries =
        context
            .encoding()
            .getApiData()
            .getExtension(JsonFgConfiguration.class, context.encoding().getCollectionId())
            .map(cfg -> Objects.equals(cfg.getUseCuries(), Boolean.TRUE))
            .orElse(false);

    if (isEnabled && context.encoding().isFeatureCollection()) {
      writeConformsTo(context.encoding().getJson());
    }

    // next chain for extensions
    next.accept(context);
  }

  @Override
  public void onFeatureStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (isEnabled && !context.encoding().isFeatureCollection()) {
      writeConformsTo(context.encoding().getJson());
    }

    // next chain for extensions
    next.accept(context);
  }

  private void writeConformsTo(JsonGenerator json) throws IOException {
    json.writeArrayFieldStart(JSON_KEY);
    json.writeString(useCuries ? CURIE_CORE : URI_CORE);
    if (has3d) {
      json.writeString(useCuries ? CURIE_3D : URI_3D);
    }
    if (hasFeatureType) {
      json.writeString(useCuries ? CURIE_TYPE : URI_TYPE);
    }
    json.writeEndArray();
  }

  private boolean isEnabled(FeatureTransformationContextGeoJson transformationContext) {
    return transformationContext
        .getApiData()
        .getExtension(JsonFgConfiguration.class, transformationContext.getCollectionId())
        .map(ExtensionConfiguration::isEnabled)
        .orElse(false);
  }

  private boolean has3d(FeatureTransformationContextGeoJson transformationContext) {
    return transformationContext.getFeatureSchemas().values().stream()
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(FeatureSchema::getPrimaryGeometry)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(
            s ->
                s.getGeometryType()
                    .map(t -> t.equals(SimpleFeatureGeometry.MULTI_POLYGON))
                    .orElse(false))
        .anyMatch(s -> s.getConstraints().map(c -> c.isClosed() && c.isComposite()).orElse(false));
  }

  private boolean hasFeatureType(FeatureTransformationContextGeoJson transformationContext) {
    return transformationContext
        .getApiData()
        .getExtension(JsonFgConfiguration.class, transformationContext.getCollectionId())
        .map(cfg -> !Objects.requireNonNullElse(cfg.getFeatureType(), ImmutableList.of()).isEmpty())
        .orElse(false);
  }
}
