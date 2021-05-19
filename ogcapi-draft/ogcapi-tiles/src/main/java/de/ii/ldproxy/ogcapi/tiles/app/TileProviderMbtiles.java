/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableMinMax;
import de.ii.ldproxy.ogcapi.tiles.domain.MinMax;
import de.ii.ldproxy.ogcapi.tiles.domain.TileProvider;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileProviderMbtiles.Builder.class)
public abstract class TileProviderMbtiles extends TileProvider {

    public final String getType() { return "MBTILES"; }

    @Nullable
    public abstract String getFilename();

    // TODO MBTILES: derive from metadata
    @JsonIgnore
    @Value.Auxiliary
    @Value.Derived
    public Map<String, MinMax> getZoomLevels() {
        return ImmutableMap.of("WebMercatorQuad", new ImmutableMinMax.Builder()
                .min(0)
                .max(14)
                .getDefault(7)
                .build());
    }

    // TODO MBTILES: derive from metadata
    @JsonIgnore
    @Value.Auxiliary
    @Value.Derived
    public String getTileEncoding() {
        return "MVT";
    }

    // TODO MBTILES: derive from metadata
    @Nullable
    @JsonIgnore
    @Value.Auxiliary
    @Value.Derived
    public double[] getCenter() {
        return new double[]{-1.5, 53.0};
    }

    @Override
    @JsonIgnore
    @Value.Auxiliary
    @Value.Derived
    public boolean requiresQuerySupport() { return false; }

    @Override
    public TileProvider mergeInto(TileProvider src) {
        if (Objects.isNull(src) || !(src instanceof TileProviderMbtiles))
            return this;

        return ImmutableTileProviderMbtiles.builder()
                                           .from((TileProviderMbtiles) src)
                                           .from(this)
                                           .build();
    }
}
