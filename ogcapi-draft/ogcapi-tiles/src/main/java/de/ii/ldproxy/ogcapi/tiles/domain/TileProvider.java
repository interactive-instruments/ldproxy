/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.ii.ldproxy.ogcapi.tiles.app.TileProviderFeatures;
import de.ii.ldproxy.ogcapi.tiles.app.TileProviderMbtiles;
import org.immutables.value.Value;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TileProviderFeatures.class, name = "FEATURES"),
        @JsonSubTypes.Type(value = TileProviderMbtiles.class, name = "MBTILES")
})
public abstract class TileProvider {

    @JsonIgnore
    @Value.Auxiliary
    @Value.Derived
    public boolean requiresQuerySupport() { return true; }

    public abstract TileProvider mergeInto(TileProvider tileProvider);
}
