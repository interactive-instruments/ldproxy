/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
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

    public PathParameterTileMatrixSetId(@Requires ExtensionRegistry extensionRegistry,
                                        @Requires FeaturesCoreProviders providers,
                                        @Requires FeatureProcessInfo featureProcessInfo) {
        this.extensionRegistry = extensionRegistry;
        this.providers = providers;
        this.featureProcessInfo = featureProcessInfo;
    }

    @Override
    public String getPattern() {
        return TMS_REGEX;
    }

    @Override
    public List<String> getValues(OgcApiDataV2 apiData) {
        List<String> tmsSetMultiCollection = apiData.getExtension(TilesConfiguration.class)
                                                    .filter(TilesConfiguration::isEnabled)
                                                    .filter(TilesConfiguration::isMultiCollectionEnabled)
                                                    .map(TilesConfiguration::getZoomLevelsDerived)
                                                    .map(Map::keySet)
                                                    .orElse(ImmutableSet.of())
                                                    .stream()
                                                    .collect(Collectors.toUnmodifiableList());

        Set<String> tmsSet = apiData.getCollections()
                .values()
                .stream()
                .filter(collection -> apiData.isCollectionEnabled(collection.getId()))
                .map(collection -> collection.getExtension(TilesConfiguration.class))
                .filter(config -> config.filter(ExtensionConfiguration::isEnabled).isPresent())
                .map(config -> config.get().getZoomLevelsDerived().keySet())
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        tmsSet.addAll(tmsSetMultiCollection);

        return extensionRegistry.getExtensionsForType(TileMatrixSet.class).stream()
                                .map(TileMatrixSet::getId)
                                .filter(tmsSet::contains)
                                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        if (!schemaMap.containsKey(apiData.hashCode())) {
            schemaMap.put(apiData.hashCode(),new StringSchema()._enum(ImmutableList.copyOf(getValues(apiData))));
        }

        return schemaMap.get(apiData.hashCode());
    }

    @Override
    public String getName() {
        return "tileMatrixSetId";
    }

    @Override
    public String getDescription() {
        return "The local identifier of a tile matrix set, unique within the API.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId) {
        if (isApplicable(apiData, definitionPath))
            return false;

        return apiData.getCollections()
                      .get(collectionId)
                      .getExtension(TilesConfiguration.class)
                      .map(ExtensionConfiguration::isEnabled).orElse(true);
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
                (definitionPath.startsWith("/tileMatrixSets/{tileMatrixSetId}") ||
                 definitionPath.startsWith("/collections/{collectionId}/tiles/{tileMatrixSetId}") ||
                 definitionPath.startsWith("/tiles/{tileMatrixSetId}"));
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }
}
