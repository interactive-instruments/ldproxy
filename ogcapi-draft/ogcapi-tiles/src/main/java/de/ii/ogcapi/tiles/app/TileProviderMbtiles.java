/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.tiles.domain.MinMax;
import de.ii.ogcapi.tiles.domain.Tile;
import de.ii.ogcapi.tiles.domain.TileProvider;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileMbtilesTile.Builder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileProviderMbtiles.Builder.class)
public abstract class TileProviderMbtiles extends TileProvider {

    public final String getType() { return "MBTILES"; }

    @Nullable
    public abstract String getFilename();

    @JsonIgnore
    public abstract Map<String, MinMax> getZoomLevels();

    @Nullable
    @JsonIgnore
    public abstract String getTileEncoding();

    @JsonIgnore
    @Value.Auxiliary
    @Value.Derived
    public List<String> getTileEncodings() { return Objects.nonNull(getTileEncoding()) ? ImmutableList.of(getTileEncoding()) : ImmutableList.of(); }

    @JsonIgnore
    public abstract List<Double> getCenter();

    @Override
    @JsonIgnore
    @Value.Default
    public boolean requiresQuerySupport() { return false; }

    @Override
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    public boolean isMultiCollectionEnabled() {
        return true;
    }

    @Override
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    public boolean isSingleCollectionEnabled() {
        return false;
    }

    @Override
    public TileProvider mergeInto(TileProvider source) {
        if (Objects.isNull(source) || !(source instanceof TileProviderMbtiles))
            return this;

        TileProviderMbtiles src = (TileProviderMbtiles) source;

        ImmutableTileProviderMbtiles.Builder builder = ImmutableTileProviderMbtiles.builder()
                                                                                   .from(src)
                                                                                   .from(this);

        if (!getCenter().isEmpty())
            builder.center(getCenter());
        else if (!src.getCenter().isEmpty())
            builder.center(src.getCenter());

        return builder.build();
    }

    @Override
    @JsonIgnore
    @Value.Derived
    public QueryInput getQueryInput(OgcApiDataV2 apiData, URICustomizer uriCustomizer,
                                    Map<String, String> queryParameters, List<OgcApiQueryParameter> allowedParameters,
                                    QueryInput genericInput, Tile tile) {

        return new Builder()
            .from(genericInput)
            .tile(tile)
            .provider(this)
            .build();
    }
}
