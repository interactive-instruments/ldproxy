/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformations;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableTilesConfiguration.Builder.class)
public interface TilesConfiguration extends ExtensionConfiguration, FeatureTransformations {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    Optional<String> getFeatureProvider();

    @Nullable
    Integer getLimit();

    @Nullable
    Integer getMaxPointPerTileDefault();

    @Nullable
    Integer getMaxLineStringPerTileDefault();

    @Nullable
    Integer getMaxPolygonPerTileDefault();

    @Nullable
    Boolean getSingleCollectionEnabled();

    @Nullable
    Boolean getMultiCollectionEnabled();

    @Nullable
    Boolean getIgnoreInvalidGeometries();

    @Nullable
    Map<String, MinMax> getSeeding();

    @Nullable
    Map<String, MinMax> getZoomLevels();

    @Nullable
    Map<String, MinMax> getZoomLevelsCache();

    @Nullable
    Map<String, List<PredefinedFilter>> getFilters();

    List<String> getTileEncodings();

    List<String> getTileSetEncodings();

    @Nullable
    double[] getCenter();

    @Override
    default Builder getBuilder() {
        return new ImmutableTilesConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableTilesConfiguration.Builder builder = ((ImmutableTilesConfiguration.Builder) source.getBuilder())
                .from(source)
                .from(this);

        //TODO: this is a work-around for default from behaviour (map is not reset, which leads to duplicates in ImmutableMap)
        // try to find a better solution that also enables deep merges
        if (!getTileEncodings().isEmpty())
            builder.tileEncodings(getTileEncodings());
        if (!getTileSetEncodings().isEmpty())
            builder.tileSetEncodings(getTileSetEncodings());
        if (getSeeding()!=null)
            builder.seeding(getSeeding());
        if (getZoomLevels()!=null)
            builder.zoomLevels(getZoomLevels());
        if (getZoomLevelsCache()!=null)
            builder.zoomLevelsCache(getZoomLevelsCache());
        if (getFilters()!=null)
            builder.filters(getFilters());

        return builder.build();
    }

    /**
     *
     * @return seeding map also considering the zoom level configuration (drops zoom levels outside of the range from seeding)
     */
    @Value.Derived
    default Map<String, MinMax> getEffectiveSeeding() {
        Map<String, MinMax> baseSeeding = getSeeding();
        if (Objects.isNull(baseSeeding))
            return ImmutableMap.of();

        Map<String, MinMax> zoomLevels = getZoomLevels();
        if (Objects.isNull(zoomLevels))
            return ImmutableMap.of();

        ImmutableMap.Builder<String, MinMax> responseBuilder = ImmutableMap.builder();
        for (Map.Entry<String, MinMax> entry : baseSeeding.entrySet()) {
            if (zoomLevels.containsKey(entry.getKey())) {
                MinMax minmax = zoomLevels.get(entry.getKey());
                int minSeeding = entry.getValue().getMin();
                int maxSeeding = entry.getValue().getMax();
                int minRange = minmax.getMin();
                int maxRange = minmax.getMax();
                if (maxSeeding >= minRange && minSeeding <= maxRange)
                    responseBuilder.put(entry.getKey(), new ImmutableMinMax.Builder()
                                                                           .min(Math.max(minSeeding, minRange))
                                                                           .max(Math.min(maxSeeding, maxRange))
                                                                           .build());
            }
        }

        return responseBuilder.build();
    }

}