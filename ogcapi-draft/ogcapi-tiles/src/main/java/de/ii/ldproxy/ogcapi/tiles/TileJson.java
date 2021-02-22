/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.ii.ldproxy.ogcapi.tiles;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(as = ImmutableTileJson.class)
@JsonPropertyOrder({
    "tilejson",
    "tiles",
    "vector_layers",
    "attribution",
    "bounds",
    "center",
    "data",
    "description",
    "fillzoom",
    "grids",
    "legend",
    "maxzoom",
    "minzoom",
    "name",
    "scheme",
    "template",
    "version"
})
public abstract class TileJson
{

    @JsonProperty("tilejson")
    @Value.Default
    public String getTilejson() { return "3.0.0"; };

    @JsonProperty("tiles")
    public abstract List<String> getTiles();

    @JsonProperty("vector_layers")
    public abstract List<VectorLayer> getVectorLayers();

    @JsonProperty("attribution")
    public abstract Optional<String> getAttribution();

    @JsonProperty("bounds")
    public abstract List<Double> getBounds();

    @JsonProperty("center")
    public abstract List<Number> getCenter();

    @JsonProperty("data")
    public abstract List<String> getData();

    @JsonProperty("description")
    public abstract Optional<String> getDescription();

    @JsonProperty("fillzoom")
    public abstract Optional<Integer> getFillzoom();

    @JsonProperty("grids")
    public abstract List<String> getGrids();

    @JsonProperty("legend")
    public abstract Optional<String> getLegend();

    @JsonProperty("maxzoom")
    public abstract Optional<Integer> getMaxzoom();

    @JsonProperty("minzoom")
    public abstract Optional<Integer> getMinzoom();

    @JsonProperty("name")
    public abstract Optional<String> getName();

    @JsonProperty("scheme")
    public abstract Optional<String> getScheme();

    @JsonProperty("template")
    public abstract Optional<String> getTemplate();

    @JsonProperty("version")
    public abstract Optional<String> getVersion();

    @JsonAnyGetter
    public abstract Map<String, Object> getAdditionalProperties();
}
