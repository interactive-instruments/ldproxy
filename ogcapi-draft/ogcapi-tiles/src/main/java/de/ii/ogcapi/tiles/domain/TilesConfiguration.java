/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import static de.ii.ogcapi.tiles.app.CapabilityTiles.MINIMUM_SIZE_IN_PIXEL;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.ogcapi.tiles.app.TileProviderFeatures;
import de.ii.ogcapi.tiles.app.TileProviderMbtiles;
import de.ii.ogcapi.tiles.app.TileProviderTileServer;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableTilesConfiguration.Builder.class)
public interface TilesConfiguration extends ExtensionConfiguration, PropertyTransformations, CachingConfiguration {

    enum TileCacheType { FILES, MBTILES, NONE }

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Nullable
    TileProvider getTileProvider(); // TODO add TileServer support

    List<String> getTileSetEncodings();

    @Nullable
    TileCacheType getCache();

    @Nullable
    MapClient.Type getMapClientType();

    @Nullable
    String getStyle();

    @Nullable
    Boolean getRemoveZoomLevelConstraints();

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
                        getTileProvider().getTileEncodings() :
                        getTileProvider() instanceof TileProviderMbtiles && Objects.nonNull(((TileProviderMbtiles) getTileProvider()).getTileEncoding()) ?
                                ImmutableList.of(((TileProviderMbtiles) getTileProvider()).getTileEncoding()) :
                                getTileProvider() instanceof TileProviderTileServer && Objects.nonNull(((TileProviderTileServer) getTileProvider()).getTileEncodings()) ?
                                    getTileProvider().getTileEncodings() :
                                    ImmutableList.of();
    }

    @Deprecated
    List<Double> getCenter();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default List<Double> getCenterDerived() {
        return !getCenter().isEmpty() ?
                getCenter() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getCenter() :
                        getTileProvider() instanceof TileProviderMbtiles ?
                                ((TileProviderMbtiles) getTileProvider()).getCenter() :
                                ImmutableList.of();
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

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default Set<String> getTileMatrixSets() {
        return getZoomLevelsDerived().keySet();
    }

    @Deprecated
    @Nullable
    Boolean getSingleCollectionEnabled();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default boolean isSingleCollectionEnabled() {
        return Objects.equals(getSingleCollectionEnabled(), true)
                || (Objects.nonNull(getTileProvider()) && getTileProvider().isSingleCollectionEnabled())
                || isEnabled();
    }

    @Deprecated
    @Nullable
    Boolean getMultiCollectionEnabled();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default boolean isMultiCollectionEnabled() {
        return Objects.equals(getMultiCollectionEnabled(), true)
            || (Objects.nonNull(getTileProvider()) && getTileProvider().isMultiCollectionEnabled())
            || isEnabled();
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

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default Optional<SeedingOptions> getSeedingOptions() {
        return getTileProvider() instanceof TileProviderFeatures
            ? ((TileProviderFeatures) getTileProvider()).getSeedingOptions()
            : Optional.empty();
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
    Boolean getIgnoreInvalidGeometries();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default boolean isIgnoreInvalidGeometriesDerived() {
        return Objects.equals(getIgnoreInvalidGeometries(), true)
            || (getTileProvider() instanceof TileProviderFeatures && ((TileProviderFeatures) getTileProvider()).isIgnoreInvalidGeometries())
            || isEnabled();
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
    Optional<Double> getMaxRelativeAreaChangeInPolygonRepair();

    @Deprecated
    Optional<Double> getMaxAbsoluteAreaChangeInPolygonRepair();

    @Deprecated
    @Nullable
    Double getMinimumSizeInPixel();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default double getMinimumSizeInPixelDerived() {
        return Objects.requireNonNullElse(getMinimumSizeInPixel(),
            Objects.requireNonNullElse(getTileProvider() instanceof TileProviderFeatures
                    ? ((TileProviderFeatures) getTileProvider()).getMinimumSizeInPixel()
                    : null,
                MINIMUM_SIZE_IN_PIXEL));
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
                .from(this)
                .transformations(PropertyTransformations.super.mergeInto((PropertyTransformations) source).getTransformations());

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

        if (!getCenter().isEmpty())
            builder.center(getCenter());
        else if (!src.getCenter().isEmpty())
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

    @Value.Check
    default TilesConfiguration alwaysFlatten() {
        if (!hasTransformation(PropertyTransformations.WILDCARD, transformation -> transformation.getFlatten().isPresent())) {

            Map<String, List<PropertyTransformation>> transformations = withTransformation(PropertyTransformations.WILDCARD,
                new ImmutablePropertyTransformation.Builder()
                .flatten(".")
                .build());

            return new ImmutableTilesConfiguration.Builder()
                .from(this)
                .transformations(transformations)
                .build();
        }

        return this;
    }

}
