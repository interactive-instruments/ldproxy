/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(as = ImmutableVectorLayer.class)
@JsonPropertyOrder({"id", "fields", "description", "maxzoom", "minzoom"})
public abstract class VectorLayer {

  @JsonProperty("id")
  public abstract String getId();

  @JsonProperty("fields")
  public abstract Map<String, String> getFields();

  @JsonProperty("description")
  public abstract Optional<String> getDescription();

  @JsonProperty("geometry_type")
  public abstract Optional<String> getGeometryType();

  @JsonProperty("maxzoom")
  public abstract Optional<Number> getMaxzoom();

  @JsonProperty("minzoom")
  public abstract Optional<Number> getMinzoom();

  @JsonAnyGetter
  public abstract Map<String, Object> getAdditionalProperties();
}
