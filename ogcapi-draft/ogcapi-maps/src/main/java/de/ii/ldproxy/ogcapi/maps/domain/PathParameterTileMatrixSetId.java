/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.maps.domain;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class PathParameterTileMatrixSetId implements OgcApiPathParameter {

    public static final String TMS_REGEX = "\\w+";

    private final ExtensionRegistry extensionRegistry;
    final FeaturesCoreProviders providers;
    final FeatureProcessInfo featureProcessInfo;
    protected ConcurrentMap<Integer, Schema> schemaMap = new ConcurrentHashMap<>();
    private final TileMatrixSetRepository tileMatrixSetRepository;

    public PathParameterTileMatrixSetId(@Requires ExtensionRegistry extensionRegistry,
                                        @Requires FeaturesCoreProviders providers,
                                        @Requires FeatureProcessInfo featureProcessInfo,
                                        @Requires TileMatrixSetRepository tileMatrixSetRepository) {
        this.extensionRegistry = extensionRegistry;
        this.providers = providers;
        this.featureProcessInfo = featureProcessInfo;
        this.tileMatrixSetRepository = tileMatrixSetRepository;
    }

    @Override
    public String getPattern() {
        return TMS_REGEX;
    }

    @Override
    public List<String> getValues(OgcApiDataV2 apiData) {
        return tileMatrixSetRepository.get("WebMercatorQuad").isPresent()
            ? ImmutableList.of("WebMercatorQuad")
            : ImmutableList.of();
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        if (!schemaMap.containsKey(apiData.hashCode())) {
            schemaMap.put(apiData.hashCode(),new StringSchema()._enum(ImmutableList.copyOf(getValues(apiData))));
        }

        return schemaMap.get(apiData.hashCode());
    }

    @Override
    public String getId() {
        return "tileMatrixSetIdMapTiles";
    }

    @Override
    public String getName() {
        return "tileMatrixSetId";
    }

    @Override
    public String getDescription() {
        return "The local identifier of a tile matrix set, unique within the API. Currently only 'WebMercatorQuad' is supported.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId) {
        if (isApplicable(apiData, definitionPath))
            return false;

        return apiData.getCollections()
            .get(collectionId)
            .getExtension(MapTilesConfiguration.class)
            .map(ExtensionConfiguration::isEnabled)
            .orElse(false);
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
            (definitionPath.startsWith("/collections/{collectionId}/map/tiles/{tileMatrixSetId}") ||
                definitionPath.startsWith("/map/tiles/{tileMatrixSetId}"));
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return MapTilesConfiguration.class;
    }
}
