/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.ii.ldproxy.ogcapi.domain.CachingConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformations;
import de.ii.ldproxy.ogcapi.features.core.domain.PropertyTransformation;
import de.ii.ldproxy.ogcapi.tiles.app.TileProviderFeatures;
import de.ii.ldproxy.ogcapi.tiles.app.TileProviderMbtiles;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableTilesConfiguration.Builder.class)
public interface TilesConfiguration extends ExtensionConfiguration, FeatureTransformations, CachingConfiguration {

    enum TileCacheType { FILES, MBTILES }

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Nullable
    TileProvider getTileProvider();

    List<String> getTileSetEncodings();

    @Nullable
    TileCacheType getCache();

    @Deprecated
    List<String> getTileEncodings();

    // Note: Most configuration options have been moved to TileProviderFeatures and have been deprecated here.
    // The getXyzDerived() methods support the deprecated configurations as well as the new style.

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default List<String> getTileEncodingsDerived() {
        return !getTileEncodings().isEmpty() ?
                getTileEncodings() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getTileEncodings() :
                        getTileProvider() instanceof TileProviderMbtiles && Objects.nonNull(((TileProviderMbtiles) getTileProvider()).getTileEncoding()) ?
                                ImmutableList.of(((TileProviderMbtiles) getTileProvider()).getTileEncoding()) :
                                ImmutableList.of();
    }

    @Deprecated
    @Nullable
    List<Double> getCenter();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    @Nullable
    default List<Double> getCenterDerived() {
        return Objects.nonNull(getCenter()) ?
                getCenter() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getCenter() :
                        getTileProvider() instanceof TileProviderMbtiles ?
                                ((TileProviderMbtiles) getTileProvider()).getCenter() :
                                null;
    }

    @Deprecated
    Map<String, MinMax> getZoomLevels();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default Map<String, MinMax> getZoomLevelsDerived() {
        return !getZoomLevels().isEmpty() ?
                getZoomLevels() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getZoomLevels() :
                        getTileProvider() instanceof TileProviderMbtiles ?
                                ((TileProviderMbtiles) getTileProvider()).getZoomLevels() :
                                ImmutableMap.of();
    }

    @Deprecated
    @Nullable
    Boolean getSingleCollectionEnabled();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    @Nullable
    default Boolean getSingleCollectionEnabledDerived() {
        return Objects.nonNull(getSingleCollectionEnabled()) ?
                getSingleCollectionEnabled() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getSingleCollectionEnabled() :
                        getEnabled();
    }

    @Deprecated
    @Nullable
    Boolean getMultiCollectionEnabled();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    @Nullable
    default Boolean getMultiCollectionEnabledDerived() {
        return Objects.nonNull(getMultiCollectionEnabled()) ?
                getMultiCollectionEnabled() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getMultiCollectionEnabled() :
                        getEnabled();
    }

    @Deprecated
    Map<String, MinMax> getZoomLevelsCache();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default Map<String, MinMax> getZoomLevelsCacheDerived() {
        return !getZoomLevelsCache().isEmpty() ?
                getZoomLevelsCache() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getZoomLevelsCache() :
                        ImmutableMap.of();
    }

    @Deprecated
    Map<String, MinMax> getSeeding();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default Map<String, MinMax> getSeedingDerived() {
        return !getSeeding().isEmpty() ?
                getSeeding() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getSeeding() :
                        ImmutableMap.of();
    }

    @Deprecated
    @Nullable
    Integer getLimit();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    @Nullable
    default Integer getLimitDerived() {
        return Objects.nonNull(getLimit()) ?
                getLimit() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getLimit() :
                        null;
    }

    @Deprecated
    @Nullable
    Integer getMaxPointPerTileDefault();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    @Nullable
    default Integer getMaxPointPerTileDefaultDerived() {
        return Objects.nonNull(getMaxPointPerTileDefault()) ?
                getMaxPointPerTileDefault() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getMaxPointPerTileDefault() :
                        null;
    }

    @Deprecated
    @Nullable
    Integer getMaxLineStringPerTileDefault();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    @Nullable
    default Integer getMaxLineStringPerTileDefaultDerived() {
        return Objects.nonNull(getMaxLineStringPerTileDefault()) ?
                getMaxLineStringPerTileDefault() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getMaxLineStringPerTileDefault() :
                        null;
    }

    @Deprecated
    @Nullable
    Integer getMaxPolygonPerTileDefault();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    @Nullable
    default Integer getMaxPolygonPerTileDefaultDerived() {
        return Objects.nonNull(getMaxPolygonPerTileDefault()) ?
                getMaxPolygonPerTileDefault() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getMaxPolygonPerTileDefault() :
                        null;
    }

    @Deprecated
    @Nullable
    Boolean getIgnoreInvalidGeometries();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    @Nullable
    default Boolean getIgnoreInvalidGeometriesDerived() {
        return Objects.nonNull(getIgnoreInvalidGeometries()) ?
                getIgnoreInvalidGeometries() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getIgnoreInvalidGeometries() :
                        null;
    }

    @Deprecated
    Map<String, List<PredefinedFilter>> getFilters();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default Map<String, List<PredefinedFilter>> getFiltersDerived() {
        return !getFilters().isEmpty() ?
                getFilters() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getFilters() :
                        ImmutableMap.of();
    }

    @Deprecated
    Map<String, List<Rule>> getRules();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default Map<String, List<Rule>> getRulesDerived() {
        return !getRules().isEmpty() ?
                getRules() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getRules() :
                        ImmutableMap.of();
    }

    @Deprecated
    @Nullable
    Double getMaxRelativeAreaChangeInPolygonRepair();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    @Nullable
    default Double getMaxRelativeAreaChangeInPolygonRepairDerived() {
        return Objects.nonNull(getMaxRelativeAreaChangeInPolygonRepair()) ?
                getMaxRelativeAreaChangeInPolygonRepair() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getMaxRelativeAreaChangeInPolygonRepair() :
                        null;
    }

    @Deprecated
    @Nullable
    Double getMaxAbsoluteAreaChangeInPolygonRepair();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    @Nullable
    default Double getMaxAbsoluteAreaChangeInPolygonRepairDerived() {
        return Objects.nonNull(getMaxAbsoluteAreaChangeInPolygonRepair()) ?
                getMaxAbsoluteAreaChangeInPolygonRepair() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getMaxAbsoluteAreaChangeInPolygonRepair() :
                        null;
    }

    @Deprecated
    @Nullable
    Double getMinimumSizeInPixel();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    @Nullable
    default Double getMinimumSizeInPixelDerived() {
        return Objects.nonNull(getMinimumSizeInPixel()) ?
                getMinimumSizeInPixel() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getMinimumSizeInPixel() :
                        null;
    }

    @Override
    default Builder getBuilder() {
        return new ImmutableTilesConfiguration.Builder();
    }

    @Override
    @SuppressWarnings("deprecation")
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableTilesConfiguration.Builder builder = ((ImmutableTilesConfiguration.Builder) source.getBuilder())
                .from(source)
                .from(this);

        TilesConfiguration src = (TilesConfiguration) source;

        if (Objects.nonNull(getTileProvider()) && Objects.nonNull(src.getTileProvider()))
            builder.tileProvider(getTileProvider().mergeInto(src.getTileProvider()));

        List<String> tileEncodings = Objects.nonNull(src.getTileEncodings()) ? Lists.newArrayList(src.getTileEncodings()) : Lists.newArrayList();
        getTileEncodings().forEach(tileEncoding -> {
            if (!tileEncodings.contains(tileEncoding)) {
                tileEncodings.add(tileEncoding);
            }
        });
        builder.tileEncodings(tileEncodings);

        List<String> tileSetEncodings = Objects.nonNull(src.getTileSetEncodings()) ? Lists.newArrayList(src.getTileSetEncodings()) : Lists.newArrayList();
        getTileSetEncodings().forEach(tileSetEncoding -> {
            if (!tileSetEncodings.contains(tileSetEncoding)) {
                tileSetEncodings.add(tileSetEncoding);
            }
        });
        builder.tileSetEncodings(tileSetEncodings);

        Map<String, PropertyTransformation> mergedTransformations = Objects.nonNull(src.getTransformations()) ? Maps.newLinkedHashMap(src.getTransformations()) : Maps.newLinkedHashMap();
        getTransformations().forEach((key, transformation) -> {
            if (mergedTransformations.containsKey(key)) {
                mergedTransformations.put(key, transformation.mergeInto(mergedTransformations.get(key)));
            } else {
                mergedTransformations.put(key, transformation);
            }
        });
        builder.transformations(mergedTransformations);

        Map<String, MinMax> mergedSeeding = Objects.nonNull(src.getSeeding()) ? Maps.newLinkedHashMap(src.getSeeding()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getSeeding()))
            getSeeding().forEach(mergedSeeding::put);
        builder.seeding(mergedSeeding);

        Map<String, MinMax> mergedZoomLevels = Objects.nonNull(src.getZoomLevels()) ? Maps.newLinkedHashMap(src.getZoomLevels()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getZoomLevels()))
            getZoomLevels().forEach(mergedZoomLevels::put);
        builder.zoomLevels(mergedZoomLevels);

        Map<String, MinMax> mergedZoomLevelsCache = Objects.nonNull(src.getZoomLevelsCache()) ? Maps.newLinkedHashMap(src.getZoomLevelsCache()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getZoomLevelsCache()))
            getZoomLevelsCache().forEach(mergedZoomLevelsCache::put);
        builder.zoomLevelsCache(mergedZoomLevelsCache);

        Map<String, List<Rule>> mergedRules = Objects.nonNull(src.getRules()) ? Maps.newLinkedHashMap(src.getRules()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getRules()))
            getRules().forEach(mergedRules::put);
        builder.rules(mergedRules);

        Map<String, List<PredefinedFilter>> mergedFilters = Objects.nonNull(src.getFilters()) ? Maps.newLinkedHashMap(src.getFilters()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getFilters()))
            getFilters().forEach(mergedFilters::put);
        builder.filters(mergedFilters);

        if (Objects.nonNull(getCenter()))
            builder.center(getCenter());
        else if (Objects.nonNull(src.getCenter()))
            builder.center(src.getCenter());

        return builder.build();
    }

    /**
     * @return seeding map also considering the zoom level configuration (drops zoom levels outside of the range from seeding)
     */
    @JsonIgnore
    @Value.Lazy
    default Map<String, MinMax> getEffectiveSeeding() {
        Map<String, MinMax> baseSeeding = getSeedingDerived();
        if (baseSeeding.isEmpty())
            return baseSeeding;

        Map<String, MinMax> zoomLevels = getZoomLevelsDerived();

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