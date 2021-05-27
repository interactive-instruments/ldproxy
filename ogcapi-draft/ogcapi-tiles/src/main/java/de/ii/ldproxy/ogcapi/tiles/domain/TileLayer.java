/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.MetadataDates;
import de.ii.ldproxy.ogcapi.domain.PageRepresentation;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableStyleLayer;
import de.ii.ldproxy.ogcapi.styles.domain.StyleEntry;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TilesBoundingBox;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableTileLayer.class)
public abstract class TileLayer extends PageRepresentation {

    public enum GeometryType { points, lines, polygons }

    @Override
    @JsonProperty("abstract")
    public abstract Optional<String> getDescription();

    public abstract String getId();

    public abstract List<String> getKeywords();

    public abstract Optional<String> getPointOfContact();

    public abstract Optional<MetadataDates> getDates();

    public abstract Optional<TileSet.DataType> getDataType();

    public abstract Optional<GeometryType> getGeometryType();

    public abstract Optional<String> getFeatureType();

    public abstract Optional<String> getPublisher();

    public abstract Optional<String> getTheme();

    public abstract Optional<String> getMinTileMatrix();
    public abstract Optional<String> getMaxTileMatrix();

    public abstract Optional<TilesBoundingBox> getBoundingBox();

    public abstract Optional<StyleEntry> getStyle();

    @JsonIgnore
    public abstract Map<String, Object> getAttributes();

    public abstract Optional<Link> getSampleData();

    @Value.Derived
    @Value.Auxiliary
    public Map<String, Object> getPropertiesSchema() {
        return ImmutableMap.of("type", "object", "properties", getAttributes());
    }

    @JsonIgnore
    @Value.Lazy
    public Optional<String> getAttributeList() {
        return getAttributes().isEmpty() ? Optional.empty() : Optional.of(String.join(", ", getAttributes().keySet().stream().sorted().collect(Collectors.toUnmodifiableList())));
    }
}
