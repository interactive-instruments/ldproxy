/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileMbtilesTile;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileTileServerTile;
import de.ii.ldproxy.ogcapi.tiles.domain.Tile;
import de.ii.ldproxy.ogcapi.tiles.domain.TileProvider;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileProviderTileServer.Builder.class)
public abstract class TileProviderTileServer extends TileProvider {

    public final String getType() { return "TILESERVER"; }

    // TODO add optional support for multiple styles once the specification is stable

    @Nullable
    public abstract String getUrlTemplate();

    @Nullable
    public abstract String getUrlTemplateSingleCollection();

    @Override
    public abstract List<String> getTileEncodings();

    // TODO support caching

    @Override
    @JsonIgnore
    public QueryInput getQueryInput(OgcApiDataV2 apiData, URICustomizer uriCustomizer,
                                    Map<String, String> queryParameters, List<OgcApiQueryParameter> allowedParameters,
                                    QueryInput genericInput, Tile tile) {

        return new ImmutableQueryInputTileTileServerTile.Builder()
            .from(genericInput)
            .tile(tile)
            .provider(this)
            .build();
    }

    @Override
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    public boolean isMultiCollectionEnabled() {
        return Objects.nonNull(getUrlTemplate());
    }

    @Override
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    public boolean isSingleCollectionEnabled() {
        return Objects.nonNull(getUrlTemplateSingleCollection());
    }

    @Override
    public TileProvider mergeInto(TileProvider source) {
        if (Objects.isNull(source) || !(source instanceof TileProviderTileServer))
            return this;

        TileProviderTileServer src = (TileProviderTileServer) source;

        ImmutableTileProviderTileServer.Builder builder = ImmutableTileProviderTileServer.builder()
            .from(src)
            .from(this);

        List<String> tileEncodings = Objects.nonNull(src.getTileEncodings()) ? Lists.newArrayList(src.getTileEncodings()) : Lists.newArrayList();
        getTileEncodings().forEach(tileEncoding -> {
            if (!tileEncodings.contains(tileEncoding)) {
                tileEncodings.add(tileEncoding);
            }
        });
        builder.tileEncodings(tileEncodings);

        return builder.build();
    }
}
