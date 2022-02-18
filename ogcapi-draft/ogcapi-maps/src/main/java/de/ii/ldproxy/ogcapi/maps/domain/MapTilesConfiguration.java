/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.maps.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TileProvider;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableMapTilesConfiguration.Builder.class)
public interface MapTilesConfiguration extends ExtensionConfiguration {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Nullable
    TileProvider getMapProvider(); // TODO: must be TileServer, generalize and extend to MBTiles

    @Nullable
    TilesConfiguration.TileCacheType getCache(); // TODO: add caching support

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default List<String> getTileEncodingsDerived() {
        if (Objects.isNull(getMapProvider()))
            return ImmutableList.of();
        return Objects.requireNonNullElse(getMapProvider().getTileEncodings(), ImmutableList.of());
    }

    @JsonIgnore
    @Value.Auxiliary
    @Value.Derived
    default boolean isMultiCollectionEnabled() {
        if (Objects.isNull(getMapProvider()))
            return false;
        return getMapProvider().isMultiCollectionEnabled();
    }

    @JsonIgnore
    @Value.Auxiliary
    @Value.Derived
    default boolean isSingleCollectionEnabled() {
        if (Objects.isNull(getMapProvider()))
            return false;
        return getMapProvider().isSingleCollectionEnabled();
    }

    @Override
    default MapTilesConfiguration.Builder getBuilder() {
        return new ImmutableMapTilesConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        if (Objects.isNull(source) || !(source instanceof MapTilesConfiguration))
            return this;

        MapTilesConfiguration src = (MapTilesConfiguration) source;

        ImmutableMapTilesConfiguration.Builder builder = new ImmutableMapTilesConfiguration.Builder()
            .from(src)
            .from(this);

        return builder.build();
    }
}
