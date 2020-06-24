/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableTilesConfiguration.Builder.class)
public abstract class TilesConfiguration implements ExtensionConfiguration {

    private static final int LIMIT_DEFAULT = 100000;
    private static final int MAX_POLYGON_PER_TILE_DEFAULT = 10000;
    private static final int MAX_LINE_STRING_PER_TILE_DEFAULT = 10000;
    private static final int MAX_POINT_PER_TILE_DEFAULT = 10000;

    @Value.Default
    @Override
    public boolean getEnabled() {
        return false;
    }

    @Value.Default
    public int getLimit() {
        return LIMIT_DEFAULT;
    }

    @Value.Default
    public int getMaxPointPerTileDefault() {
        return MAX_POINT_PER_TILE_DEFAULT;
    }

    @Value.Default
    public int getMaxLineStringPerTileDefault() {
        return MAX_LINE_STRING_PER_TILE_DEFAULT;
    }

    @Value.Default
    public int getMaxPolygonPerTileDefault() {
        return MAX_POLYGON_PER_TILE_DEFAULT;
    }

    @Value.Default
    public boolean getMultiCollectionEnabled() {
        return false;
    }

    @JsonMerge(value = OptBoolean.FALSE)
    @Nullable
    public abstract List<String> getFormats();

    @Nullable
    public abstract Map<String, MinMax> getSeeding();

    @Nullable
    public abstract Map<String, MinMax> getZoomLevels();

    @Nullable
    public abstract Map<String, List<PredefinedFilter>> getFilters();

    @Nullable
    public abstract double[] getCenter();

    @Override
    public <T extends ExtensionConfiguration> T mergeDefaults(T baseConfiguration) {
        boolean enabled = this.getEnabled();
        Map<String, MinMax> seeding = this.getSeeding();
        Map<String, MinMax> zoomLevels = this.getZoomLevels();
        Map<String, List<PredefinedFilter>> predefFilters = this.getFilters();
        int limit = this.getLimit();
        double[] center = this.getCenter();
        ImmutableTilesConfiguration.Builder configBuilder = new ImmutableTilesConfiguration.Builder().from(baseConfiguration);

        if (Objects.nonNull(enabled))
            configBuilder.enabled(enabled);
        if (Objects.nonNull(seeding))
            configBuilder.seeding(seeding);
        if (Objects.nonNull(zoomLevels))
            configBuilder.zoomLevels(zoomLevels);
        if (Objects.nonNull(predefFilters))
            configBuilder.filters(predefFilters);
        if (Objects.nonNull(center))
            configBuilder.center(center);
        if (Objects.nonNull(limit))
            configBuilder.limit(limit);

        return (T) configBuilder.build();
    }
}