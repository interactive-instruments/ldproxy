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
import de.ii.ldproxy.ogcapi.tiles.domain.MinMax;
import de.ii.ldproxy.ogcapi.tiles.domain.TileProvider;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileProviderMbtiles.Builder.class)
public abstract class TileProviderMbtiles extends TileProvider {

    public final String getType() { return "MBTILES"; }

    @Nullable
    public abstract String getFilename();

    @JsonIgnore
    @Value.Default
    public Map<String, MinMax> getZoomLevels() { return ImmutableMap.of(); }

    @Nullable
    @JsonIgnore
    public abstract String getTileEncoding();

    @JsonIgnore
    @Nullable
    public abstract List<Double> getCenter();

    @Override
    @JsonIgnore
    @Value.Default
    public boolean requiresQuerySupport() { return false; }

    @Override
    public TileProvider mergeInto(TileProvider src) {
        if (Objects.isNull(src) || !(src instanceof TileProviderMbtiles))
            return this;

        ImmutableTileProviderMbtiles.Builder builder = ImmutableTileProviderMbtiles.builder()
                                                                                   .from((TileProviderMbtiles) src)
                                                                                   .from(this);

        if (Objects.nonNull(getCenter()))
            builder.center(getCenter());
        else if (Objects.nonNull(((TileProviderMbtiles)src).getCenter()))
            builder.center(((TileProviderMbtiles)src).getCenter());

        return builder.build();
    }
}
