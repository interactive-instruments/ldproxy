/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ogcapi.features.geojson.app.FeaturesFormatGeoJson;
import de.ii.ogcapi.features.geojson.app.JsonGeneratorDebug;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.xtraplatform.features.json.domain.GeoJsonGeometryType;
import de.ii.xtraplatform.geometries.domain.ImmutableCoordinatesTransformer;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class FeatureTransformationContextGeoJson implements FeatureTransformationContext {

  @Override
  @Value.Default
  public ModifiableStateGeoJson getState() {
    return ModifiableStateGeoJson.create();
  }

  public abstract GeoJsonConfiguration getGeoJsonConfig();

  @Value.Default
  public ApiMediaType getMediaType() {
    return FeaturesFormatGeoJson.MEDIA_TYPE;
  }

  @Value.Default
  public Boolean getSuppressPrimaryGeometry() {
    return false;
  }

  @Value.Default
  public Boolean getForceDefaultCrs() {
    return false;
  }

  public abstract Map<String, Object> getExtensions();

  @Value.Default
  protected JsonGenerator getJsonGenerator() {
    JsonGenerator json = null;
    try {
      json = new JsonFactory().createGenerator(getOutputStream());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    json.setCodec(new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL));
    if (getPrettify()) {
      json.useDefaultPrettyPrinter();
    }
    if (getDebug()) {
      // Zum JSON debuggen hier einschalten.
      json = new JsonGeneratorDebug(json);
    }

    return json;
  }

  // TODO: to state
  private TokenBuffer tokenBuffer;

  protected TokenBuffer getJsonBuffer() {
    return tokenBuffer;
  }

  // @Value.Derived
  private TokenBuffer createJsonBuffer() {
    TokenBuffer json = new TokenBuffer(new ObjectMapper(), false);

    if (getPrettify()) {
      json.useDefaultPrettyPrinter();
    }
    return json;
  }

  public JsonGenerator getJson() {
    return getState().isBuffering() ? getJsonBuffer() : getJsonGenerator();
  }

  public final void startBuffering() throws IOException {
    getJsonGenerator().flush();
    this.tokenBuffer = createJsonBuffer();
    getState().setIsBuffering(true);
  }

  public final void stopBuffering() throws IOException {
    if (getState().isBuffering()) {
      getState().setIsBuffering(false);
      // getJsonBuffer().serialize(getJsonGenerator());
      getJsonBuffer().close();
    }
  }

  public final void flushBuffer() throws IOException {
    if (!Objects.isNull(getJsonBuffer())) {
      getJsonBuffer().serialize(getJsonGenerator());
      getJsonBuffer().flush();
    }
  }

  @Value.Modifiable
  public abstract static class StateGeoJson extends State {

    public abstract Optional<GeoJsonGeometryType> getCurrentGeometryType();

    public abstract Optional<ImmutableCoordinatesTransformer.Builder> getCoordinatesWriterBuilder();

    @Value.Default
    public int getCurrentGeometryNestingChange() {
      return 0;
    }

    @Value.Default
    public boolean isBuffering() {
      return false;
    }

    @Value.Default
    public boolean hasMore() {
      return false;
    }

    @Value.Default
    public List<Link> getCurrentFeatureLinks() {
      return ImmutableList.of();
    }

    @Value.Default
    public List<Link> getCurrentFeatureCollectionLinks() {
      return ImmutableList.of();
    }
  }
}
