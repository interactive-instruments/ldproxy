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
import de.ii.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ogcapi.features.json.fg.domain.JsonFgConfiguration;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class JsonFgWriterGeometryDimension implements GeoJsonWriter {

  public static String JSON_KEY = "geometryDimension";

  @Inject
  JsonFgWriterGeometryDimension() {}

  @Override
  public JsonFgWriterGeometryDimension create() {
    return new JsonFgWriterGeometryDimension();
  }

  @Override
  public int getSortPriority() {
    return 160;
  }

  @Override
  public void onStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (isEnabled(context.encoding()) && context.encoding().isFeatureCollection()) {
      writeGeometryDimension(
          context.encoding(),
          context
              .encoding()
              .getFeatureSchema()
              .flatMap(FeaturesFormatJsonFg::getGeometryDimension));
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
        .filter(
            cfg ->
                cfg.getIncludeInGeoJson().contains(JsonFgConfiguration.OPTION.geometryDimension)
                    || transformationContext.getMediaType().equals(FeaturesFormatJsonFg.MEDIA_TYPE))
        .isPresent();
  }

  private void writeGeometryDimension(
      FeatureTransformationContextGeoJson transformationContext,
      Optional<Integer> geometryDimension)
      throws IOException {
    if (geometryDimension.isPresent()) {
      JsonGenerator json = transformationContext.getJson();
      json.writeNumberField(JSON_KEY, geometryDimension.get());
    }
  }
}
