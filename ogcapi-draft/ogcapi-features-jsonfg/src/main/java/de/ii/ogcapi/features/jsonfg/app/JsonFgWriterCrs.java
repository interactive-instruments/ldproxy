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
import de.ii.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class JsonFgWriterCrs implements GeoJsonWriter {

  public static String JSON_KEY = "coordRefSys";

  boolean isEnabled;

  @Inject
  JsonFgWriterCrs() {}

  @Override
  public JsonFgWriterCrs create() {
    return new JsonFgWriterCrs();
  }

  @Override
  public int getSortPriority() {
    return 130;
  }

  @Override
  public void onStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    isEnabled = isEnabled(context.encoding());

    if (isEnabled && context.encoding().isFeatureCollection()) {
      writeCrs(context.encoding().getJson(), context.encoding().getTargetCrs());
    }

    // next chain for extensions
    next.accept(context);
  }

  @Override
  public void onFeatureStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    if (isEnabled && !context.encoding().isFeatureCollection()) {
      writeCrs(context.encoding().getJson(), context.encoding().getTargetCrs());
    }

    // next chain for extensions
    next.accept(context);
  }

  private void writeCrs(JsonGenerator json, EpsgCrs crs) throws IOException {
    json.writeStringField(JSON_KEY, crs.toUriString());
  }

  private boolean isEnabled(FeatureTransformationContextGeoJson transformationContext) {
    return transformationContext
        .getApiData()
        .getExtension(JsonFgConfiguration.class, transformationContext.getCollectionId())
        .filter(JsonFgConfiguration::isEnabled)
        .filter(cfg -> Objects.requireNonNullElse(cfg.getCoordRefSys(), false))
        .filter(
            cfg ->
                cfg.getIncludeInGeoJson().contains(JsonFgConfiguration.OPTION.coordRefSys)
                    || transformationContext.getMediaType().equals(FeaturesFormatJsonFg.MEDIA_TYPE)
                    || transformationContext
                        .getMediaType()
                        .equals(FeaturesFormatJsonFgCompatibility.MEDIA_TYPE))
        .isPresent();
  }
}
