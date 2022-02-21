/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileEmpty;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileMultiLayer;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileSingleLayer;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableTile;
import de.ii.ldproxy.ogcapi.tiles.domain.MinMax;
import de.ii.ldproxy.ogcapi.tiles.domain.PredefinedFilter;
import de.ii.ldproxy.ogcapi.tiles.domain.Rule;
import de.ii.ldproxy.ogcapi.tiles.domain.SeedingOptions;
import de.ii.ldproxy.ogcapi.tiles.domain.Tile;
import de.ii.ldproxy.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TileFormatWithQuerySupportExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TileFromFeatureQuery;
import de.ii.ldproxy.ogcapi.tiles.domain.TileProvider;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import javax.ws.rs.NotAcceptableException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileProviderFeatures.Builder.class)
public abstract class TileProviderFeatures extends TileProvider {

    public final String getType() { return "FEATURES"; }

    @Override
    public abstract List<String> getTileEncodings();

    public abstract Map<String, MinMax> getZoomLevels();

    public abstract Map<String, MinMax> getZoomLevelsCache();

    public abstract Optional<SeedingOptions> getSeedingOptions();

    public abstract Map<String, MinMax> getSeeding();

    public abstract Map<String, List<PredefinedFilter>> getFilters();

    public abstract Map<String, List<Rule>> getRules();

    public abstract List<Double> getCenter();

    @Nullable
    public abstract Integer getLimit();

    @Nullable
    public abstract Boolean getSingleCollectionEnabled();

    @Override
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    public boolean isSingleCollectionEnabled() {
        return Objects.equals(getSingleCollectionEnabled(), true);
    }

    @Nullable
    public abstract Boolean getMultiCollectionEnabled();

    @Override
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    public boolean isMultiCollectionEnabled() {
        return Objects.equals(getMultiCollectionEnabled(), true);
    }

    @Nullable
    public abstract Boolean getIgnoreInvalidGeometries();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    public boolean isIgnoreInvalidGeometries() {
        return Objects.equals(getIgnoreInvalidGeometries(), true);
    }

    @Deprecated
    public abstract Optional<Double> getMaxRelativeAreaChangeInPolygonRepair();

    @Deprecated
    public abstract Optional<Double> getMaxAbsoluteAreaChangeInPolygonRepair();

    @Nullable
    public abstract Double getMinimumSizeInPixel();

    @Override
    @JsonIgnore
    @Value.Default
    public boolean tilesMayBeCached() { return true; }

    @Override
    @JsonIgnore
    @Value.Derived
    public QueryInput getQueryInput(OgcApiDataV2 apiData, URICustomizer uriCustomizer,
                                    Map<String, String> queryParameters, List<OgcApiQueryParameter> allowedParameters,
                                    QueryInput genericInput, Tile tile) {
        if (!tile.getFeatureProvider().map(FeatureProvider2::supportsQueries).orElse(false)) {
            throw new IllegalStateException("Tile cannot be generated. The feature provider does not support feature queries.");
        }

        TileFormatExtension outputFormat = tile.getOutputFormat();
        List<String> collections = tile.getCollectionIds();

        if (collections.isEmpty()) {
            return new ImmutableQueryInputTileEmpty.Builder()
                .from(genericInput)
                .tile(tile)
                .build();
        }

        if (!(outputFormat instanceof TileFormatWithQuerySupportExtension))
            throw new RuntimeException(String.format("Unexpected tile format without query support. Found: %s", outputFormat.getClass().getSimpleName()));

        // first execute the information that is passed as processing parameters (e.g., "properties")
        Map<String, Object> processingParameters = new HashMap<>();
        for (OgcApiQueryParameter parameter : allowedParameters) {
            processingParameters = parameter.transformContext(null, processingParameters, queryParameters, apiData);
        }

        if (tile.isDatasetTile()) {
            if (!outputFormat.canMultiLayer() && collections.size() > 1)
                throw new NotAcceptableException("The requested tile format supports only a single layer. Please select only a single collection.");

            Map<String, Tile> singleLayerTileMap = collections.stream()
                .collect(ImmutableMap.toImmutableMap(collectionId -> collectionId, collectionId -> new ImmutableTile.Builder()
                    .from(tile)
                    .collectionIds(ImmutableList.of(collectionId))
                    .isDatasetTile(false)
                    .build()));

            Map<String, FeatureQuery> queryMap = collections.stream()
                // skip collections without spatial queryable
                .filter(collectionId -> {
                    Optional<FeaturesCoreConfiguration> featuresConfiguration = apiData.getCollections()
                        .get(collectionId)
                        .getExtension(FeaturesCoreConfiguration.class);
                    return featuresConfiguration.isPresent()
                        && featuresConfiguration.get().getQueryables().isPresent()
                        && !featuresConfiguration.get().getQueryables().get().getSpatial().isEmpty();
                })
                .collect(ImmutableMap.toImmutableMap(collectionId -> collectionId, collectionId -> {
                    String featureTypeId = apiData.getCollections()
                        .get(collectionId)
                        .getExtension(FeaturesCoreConfiguration.class)
                        .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                        .orElse(collectionId);
                    TilesConfiguration layerConfiguration = apiData.getExtension(TilesConfiguration.class, collectionId).orElseThrow();
                    FeatureQuery query = ((TileFormatWithQuerySupportExtension) outputFormat).getQuery(singleLayerTileMap.get(collectionId), allowedParameters, queryParameters, layerConfiguration, uriCustomizer);
                    return ImmutableFeatureQuery.builder()
                        .from(query)
                        .type(featureTypeId)
                        .build();
                }));

            FeaturesCoreConfiguration coreConfiguration = apiData.getExtension(FeaturesCoreConfiguration.class).orElseThrow();

            return new ImmutableQueryInputTileMultiLayer.Builder()
                .from(genericInput)
                .tile(tile)
                .singleLayerTileMap(singleLayerTileMap)
                .queryMap(queryMap)
                .processingParameters(processingParameters)
                .defaultCrs(apiData.getExtension(FeaturesCoreConfiguration.class).map(FeaturesCoreConfiguration::getDefaultEpsgCrs).orElseThrow())
                .build();
        } else {
            String collectionId = tile.getCollectionId();
            FeatureTypeConfigurationOgcApi featureType = apiData.getCollectionData(collectionId).orElseThrow();
            TilesConfiguration layerConfiguration = apiData.getExtension(TilesConfiguration.class, collectionId).orElseThrow();
            FeatureQuery query = ((TileFromFeatureQuery) outputFormat).getQuery(tile, allowedParameters, queryParameters, layerConfiguration, uriCustomizer);

            FeaturesCoreConfiguration coreConfiguration = featureType.getExtension(FeaturesCoreConfiguration.class).orElseThrow();

            return new ImmutableQueryInputTileSingleLayer.Builder()
                .from(genericInput)
                .tile(tile)
                .query(query)
                .processingParameters(processingParameters)
                .defaultCrs(featureType.getExtension(FeaturesCoreConfiguration.class)
                                .map(FeaturesCoreConfiguration::getDefaultEpsgCrs)
                                .orElseThrow())
                .build();
        }
    }

    @Override
    public TileProvider mergeInto(TileProvider source) {
        if (Objects.isNull(source) || !(source instanceof TileProviderFeatures))
            return this;

        ImmutableTileProviderFeatures.Builder builder = ImmutableTileProviderFeatures.builder()
                                                                                     .from((TileProviderFeatures) source)
                                                                                     .from(this);

        TileProviderFeatures src = (TileProviderFeatures) source;

        List<String> tileEncodings = Objects.nonNull(src.getTileEncodings()) ? Lists.newArrayList(src.getTileEncodings()) : Lists.newArrayList();
        getTileEncodings().forEach(tileEncoding -> {
            if (!tileEncodings.contains(tileEncoding)) {
                tileEncodings.add(tileEncoding);
            }
        });
        builder.tileEncodings(tileEncodings);

        Map<String, MinMax> mergedSeeding = Objects.nonNull(src.getSeeding()) ? Maps.newLinkedHashMap(src.getSeeding()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getSeeding()))
            getSeeding().forEach(mergedSeeding::put);
        builder.seeding(mergedSeeding);

        Map<String, MinMax> mergedZoomLevels = Objects.nonNull(src.getZoomLevels()) ? Maps.newLinkedHashMap(src.getZoomLevels()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getZoomLevels()))
            getZoomLevels().forEach(mergedZoomLevels::put);
        builder.zoomLevels(mergedZoomLevels);

        if (!getCenter().isEmpty())
            builder.center(getCenter());
        else if (!src.getCenter().isEmpty())
            builder.center(src.getCenter());

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
}
