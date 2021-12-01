/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import static de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

import com.google.common.collect.ImmutableList;
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

public abstract class TileFormatExtension implements FormatExtension {

    protected String SCHEMA_REF_TILE = "#/components/schemas/Binary";
    protected Schema SCHEMA_TILE = new BinarySchema();

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(TilesConfiguration.class)
                      .filter(TilesConfiguration::getEnabled)
                      .filter(TilesConfiguration::isMultiCollectionEnabled)
                      .filter(config -> config.getTileEncodingsDerived().contains(this.getMediaType().label()))
                      .isPresent();
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return apiData.getExtension(TilesConfiguration.class, collectionId)
                      .filter(TilesConfiguration::getEnabled)
                      .filter(TilesConfiguration::isSingleCollectionEnabled)
                      .filter(config -> config.getTileEncodingsDerived().contains(this.getMediaType().label()))
                      .isPresent();
    }

    @Override
    public String getPathPattern() {
        return "^(?:/collections/"+COLLECTION_ID_PATTERN+")?/tiles/\\w+/\\w+/\\w+/\\w+/?$";
    }

    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        List<String> formats = apiData.getExtension(TilesConfiguration.class)
            .map(TilesConfiguration::getTileEncodingsDerived)
            .orElse(ImmutableList.of());
        return isEnabledForApi(apiData) &&
            definitionPath.startsWith("/tiles") &&
            ((formats.isEmpty() && isEnabledByDefault()) || formats.contains(getMediaType().label()));
    }

    public boolean isApplicable(OgcApiDataV2 apiData, String collectionId, String definitionPath) {
        List<String> formats = apiData.getExtension(TilesConfiguration.class, collectionId)
            .map(TilesConfiguration::getTileEncodingsDerived)
            .orElse(ImmutableList.of());
        return isEnabledForApi(apiData, collectionId) &&
            definitionPath.startsWith("/collection/{collectionId}/tiles") &&
            ((formats.isEmpty() && isEnabledByDefault()) || formats.contains(getMediaType().label()));
    }

    public boolean canMultiLayer() { return false; }

    public boolean supportsFeatureQuery() { return this instanceof TileFromFeatureQuery; }

    public abstract String getExtension();

    public boolean getGzippedInMbtiles() { return false; }

    public boolean getSupportsEmptyTile() { return false; }

    public byte[] getEmptyTile(Tile tile) {
        throw new IllegalStateException(String.format("No empty tile available for tile format %s.", this.getClass().getSimpleName()));
    }

    public abstract TileSet.DataType getDataType();

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }
}
