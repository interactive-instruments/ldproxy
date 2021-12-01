/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.maps.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.maps.app.MapProviderTileserver;
import de.ii.ldproxy.ogcapi.tiles.app.TileProviderFeatures;
import de.ii.ldproxy.ogcapi.tiles.app.TileProviderMbtiles;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MapProviderTileserver.class, name = "TILESERVER")
})
public abstract class MapProvider {

    @Value.Default
    public List<String> getTileEncodings() { return ImmutableList.of(); }

    @JsonIgnore
    @Value.Default
    public boolean requiresTileProvider() { return true; }

    public abstract MapProvider mergeInto(MapProvider mapProvider);
}
