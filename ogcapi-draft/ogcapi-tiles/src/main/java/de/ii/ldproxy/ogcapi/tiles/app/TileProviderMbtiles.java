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
    public abstract Map<String, MinMax> getZoomLevels();

    @Nullable
    @JsonIgnore
    public abstract String getTileEncoding();

    @JsonIgnore
    public abstract List<Double> getCenter();

    @Override
    @JsonIgnore
    @Value.Default
    public boolean requiresQuerySupport() { return false; }

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
}
