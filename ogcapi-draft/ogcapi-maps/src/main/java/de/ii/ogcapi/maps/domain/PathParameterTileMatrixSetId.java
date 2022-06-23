/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.maps.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class PathParameterTileMatrixSetId implements OgcApiPathParameter {

  public static final String TMS_REGEX = "\\w+";

  private final ConcurrentMap<Integer, Schema<?>> schemaMap = new ConcurrentHashMap<>();
  private final SchemaValidator schemaValidator;
  private final TileMatrixSetRepository tileMatrixSetRepository;

  @Inject
  public PathParameterTileMatrixSetId(
      SchemaValidator schemaValidator, TileMatrixSetRepository tileMatrixSetRepository) {
    this.schemaValidator = schemaValidator;
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
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    if (!schemaMap.containsKey(apiData.hashCode())) {
      schemaMap.put(
          apiData.hashCode(), new StringSchema()._enum(ImmutableList.copyOf(getValues(apiData))));
    }

    return schemaMap.get(apiData.hashCode());
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
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
    if (isApplicable(apiData, definitionPath)) return false;

    return apiData
        .getCollections()
        .get(collectionId)
        .getExtension(MapTilesConfiguration.class)
        .map(ExtensionConfiguration::isEnabled)
        .orElse(false);
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
    return isEnabledForApi(apiData)
        && (definitionPath.startsWith("/collections/{collectionId}/map/tiles/{tileMatrixSetId}")
            || definitionPath.startsWith("/map/tiles/{tileMatrixSetId}"));
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return MapTilesConfiguration.class;
  }
}
