/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import static de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.Schema;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TileFormatExtension extends FormatExtension {

    String SCHEMA_REF_TILE = "#/components/schemas/Binary";
    Schema SCHEMA_TILE = new BinarySchema();

    @Override
    default boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(TilesConfiguration.class)
                      .filter(TilesConfiguration::getEnabled)
                      .filter(TilesConfiguration::isMultiCollectionEnabled)
                      .filter(config -> config.getTileEncodingsDerived().contains(this.getMediaType().label()))
                      .isPresent();
    }

    @Override
    default boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return apiData.getCollections()
                      .get(collectionId)
                      .getExtension(TilesConfiguration.class)
                      .filter(TilesConfiguration::getEnabled)
                      .filter(TilesConfiguration::isSingleCollectionEnabled)
                      .filter(config -> config.getTileEncodingsDerived().contains(this.getMediaType().label()))
                      .isPresent();
    }

    @Override
    default String getPathPattern() {
        return "^(?:/collections/"+COLLECTION_ID_PATTERN+")?/tiles/\\w+/\\w+/\\w+/\\w+/?$";
    }

    default boolean canMultiLayer() { return false; }

    default boolean canTransformFeatures() { return false; }

    default Optional<FeatureTokenEncoder<?>> getFeatureEncoder(
        FeatureTransformationContextTiles transformationContext) {
        return Optional.empty();
    }

    default Optional<PropertyTransformations> getPropertyTransformations(
        FeatureTypeConfigurationOgcApi collectionData) {

        Optional<PropertyTransformations> coreTransformations = collectionData.getExtension(FeaturesCoreConfiguration.class)
            .map(featuresCoreConfiguration -> ((PropertyTransformations)featuresCoreConfiguration));

        Optional<PropertyTransformations> formatTransformations = collectionData.getExtension(this.getBuildingBlockConfigurationType())
            .filter(buildingBlockConfiguration -> buildingBlockConfiguration instanceof PropertyTransformations)
            .map(buildingBlockConfiguration -> ((PropertyTransformations)buildingBlockConfiguration));


        return formatTransformations.map(ft -> coreTransformations.map(ft::mergeInto).orElse(ft))
            .or(() -> coreTransformations);
    }

    default Optional<PropertyTransformations> getPropertyTransformations(
        FeatureTypeConfigurationOgcApi collectionData, Map<String, String> substitutions) {
        return getPropertyTransformations(collectionData).map(propertyTransformations -> propertyTransformations.withSubstitutions(substitutions));
    }

    String getExtension();

    default boolean getGzippedInMbtiles() { return false; }

    default boolean getSupportsEmptyTile() { return false; }

    default byte[] getEmptyTile(Tile tile) {
        throw new IllegalStateException(String.format("No empty tile available for tile format %s.", this.getClass().getSimpleName()));
    }

    TileSet.DataType getDataType();

    @Override
    default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }
}
