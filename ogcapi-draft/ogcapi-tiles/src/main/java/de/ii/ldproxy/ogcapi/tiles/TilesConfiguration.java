/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformations;
import de.ii.ldproxy.ogcapi.features.core.domain.PropertyTransformation;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
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
    Map<String, MinMax> getZoomLevels();

    @Nullable
    Map<String, MinMax> getZoomLevelsCache();

    @Nullable
    Map<String, MinMax> getSeeding();

    @Nullable
    Map<String, List<PredefinedFilter>> getFilters();

    @Nullable
    Map<String, List<Rule>> getRules();

    List<String> getTileEncodings();

    List<String> getTileSetEncodings();

    @Nullable
    double[] getCenter();

    @Nullable
    Double getMaxRelativeAreaChangeInPolygonRepair();

    @Nullable
    Double getMaxAbsoluteAreaChangeInPolygonRepair();

    @Nullable
    Double getMinimumSizeInPixel();

    @Override
    default Builder getBuilder() {
        return new ImmutableTilesConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableTilesConfiguration.Builder builder = ((ImmutableTilesConfiguration.Builder) source.getBuilder())
                .from(source)
                .from(this);

        List<String> tileEncodings = Lists.newArrayList(((TilesConfiguration) source).getTileEncodings());
        getTileEncodings().forEach(tileEncoding -> {
            if (!tileEncodings.contains(tileEncoding)) {
                tileEncodings.add(tileEncoding);
            }
        });
        builder.tileEncodings(tileEncodings);

        List<String> tileSetEncodings = Lists.newArrayList(((TilesConfiguration) source).getTileSetEncodings());
        getTileSetEncodings().forEach(tileSetEncoding -> {
            if (!tileSetEncodings.contains(tileSetEncoding)) {
                tileSetEncodings.add(tileSetEncoding);
            }
        });
        builder.tileSetEncodings(tileSetEncodings);

        Map<String, PropertyTransformation> mergedTransformations = new LinkedHashMap<>(
                ((TilesConfiguration) source).getTransformations());
        getTransformations().forEach((key, transformation) -> {
            if (mergedTransformations.containsKey(key)) {
                mergedTransformations.put(key, transformation.mergeInto(mergedTransformations.get(key)));
            } else {
                mergedTransformations.put(key, transformation);
            }
        });
        builder.transformations(mergedTransformations);

        Map<String, MinMax> mergedSeeding = new LinkedHashMap<>(
                ((TilesConfiguration) source).getSeeding());
        if (Objects.nonNull(getSeeding()))
            getSeeding().forEach(mergedSeeding::putIfAbsent);
        builder.seeding(mergedSeeding);

        Map<String, MinMax> mergedZoomLevels = new LinkedHashMap<>(
                ((TilesConfiguration) source).getZoomLevels());
        if (Objects.nonNull(getZoomLevels()))
            getZoomLevels().forEach(mergedZoomLevels::putIfAbsent);
        builder.zoomLevels(mergedZoomLevels);

        Map<String, MinMax> mergedZoomLevelsCache = new LinkedHashMap<>(
                ((TilesConfiguration) source).getZoomLevelsCache());
        if (Objects.nonNull(getZoomLevelsCache()))
            getZoomLevelsCache().forEach(mergedZoomLevelsCache::putIfAbsent);
        builder.zoomLevelsCache(mergedZoomLevelsCache);

        Map<String, List<Rule>> mergedRules = new LinkedHashMap<>(
                ((TilesConfiguration) source).getRules());
        if (Objects.nonNull(getRules()))
            getRules().forEach(mergedRules::putIfAbsent);
        builder.rules(mergedRules);

        Map<String, List<PredefinedFilter>> mergedFilters = new LinkedHashMap<>(
                ((TilesConfiguration) source).getFilters());
        if (Objects.nonNull(getFilters()))
            getFilters().forEach(mergedFilters::putIfAbsent);
        builder.filters(mergedFilters);

        return builder.build();
    }

    /**
     *
     * @return seeding map also considering the zoom level configuration (drops zoom levels outside of the range from seeding)
     */
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
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
                int minSeeding = entry.getValue()
                                      .getMin();
                int maxSeeding = entry.getValue()
                                      .getMax();
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