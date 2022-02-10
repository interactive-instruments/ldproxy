/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.tiles.app.TileProviderFeatures;
import de.ii.ldproxy.ogcapi.tiles.app.TileProviderMbtiles;
import de.ii.ldproxy.ogcapi.tiles.app.TileProviderTileServer;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TileProviderFeatures.class, name = "FEATURES"),
    @JsonSubTypes.Type(value = TileProviderMbtiles.class, name = "MBTILES"),
    @JsonSubTypes.Type(value = TileProviderTileServer.class, name = "TILESERVER")
})
public abstract class TileProvider {

    @JsonIgnore
    @Value.Default
    public boolean tilesMayBeCached() { return false; }

    @JsonIgnore
    @Value.Default
    public boolean requiresQuerySupport() { return true; }

    @JsonIgnore
    @Value.Default
    public boolean supportsTilesHints() { return false; }

    public abstract boolean isMultiCollectionEnabled();

    public abstract boolean isSingleCollectionEnabled();

    public abstract List<String> getTileEncodings();

    public abstract TileProvider mergeInto(TileProvider tileProvider);

    public abstract QueryInput getQueryInput(OgcApiDataV2 apiData, URICustomizer uriCustomizer,
                                             Map<String, String> queryParameters, List<OgcApiQueryParameter> allowedParameters,
                                             QueryInput genericInput, Tile tile);
}
