/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileTileServerTile;
import de.ii.ogcapi.tiles.domain.Tile;
import de.ii.ogcapi.tiles.domain.TileProvider;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * # Tile-Provider TILESERVER
 * @lang_en With this tile provider, the tiles are obtained from
 * [TileServer-GL instance](https://github.com/maptiler/tileserver-gl).
 * Only the "WebMercatorQuad" tile scheme is supported.
 *
 * In the current version, this provider is only supported in the [Map Tiles](map-tiles.md) module.
 * Only bitmap tile formats are supported. Seeding or caching are not supported.
 *
 * This tile provider is experimental and its configuration options may change in future versions.
 * @lang_de Bei diesem Tile-Provider werden die Kacheln über eine
 * [TileServer-GL-Instanz](https://github.com/maptiler/tileserver-gl) bezogen. Unterstützt wird nur
 * das Kachelschema "WebMercatorQuad".
 *
 * In der aktuellen Version wird dieser Provider nur im Modul [Map Tiles](map-tiles.md) unterstützt.
 * Unterstützt werden nur die Bitmap-Kachelformate. Seeding oder Caching werden nicht unterstützt.
 *
 * Dieser Tile-Provider ist experimentell und seine Konfigurationsoptionen können sich in zukünftigen
 * Versionen ändern.
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileProviderTileServer.Builder.class)
public abstract class TileProviderTileServer extends TileProvider {

    /**
     * @lang_en Fixed value, identifies the tile provider type.
     * @lang_de Fester Wert, identifiziert die Tile-Provider-Art.
     * @default `TILESERVER`
     */
    public final String getType() { return "TILESERVER"; }

    // TODO add optional support for multiple styles once the specification is stable

    /**
     * @lang_en URL template for accessing tiles. Parameters to use are
     * `{tileMatrix}`, `{tileRow}`, `{tileCol}` and `{fileExtension}`.
     * @lang_de URL-Template für den Zugriff auf Kacheln. Zu verwenden sind
     * die Parameter `{tileMatrix}`, `{tileRow}`, `{tileCol}` und `{fileExtension}`.
     * @default `null`
     */
    @Nullable
    public abstract String getUrlTemplate();

    /**
     * @lang_en URL template for accessing tiles for a collection.
     * @lang_de URL-Template für den Zugriff auf Kacheln für eine Collection.
     * @default `null`
     */
    @Nullable
    public abstract String getUrlTemplateSingleCollection();

    /**
     * @lang_en List of tile formats to be supported, allowed are `PNG`, `WebP` and `JPEG`.
     * @lang_de Liste der zu unterstützenden Kachelformate, erlaubt sind `PNG`, `WebP` und `JPEG`.
     * @default `[]`
     */
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
