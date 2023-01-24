/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetRepository;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title tileMatrixSetId
 * @endpoints Dataset Tilesets, Dataset Tileset, Dataset Tile, Collection Tilesets, Collection
 *     Tileset, Collection Tile
 * @langEn The identifier of the tiling scheme.
 * @langDe Der Identifikator des Kachelschemas.
 */
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
    List<String> tmsSetMultiCollection =
        apiData
            .getExtension(TilesConfiguration.class)
            .filter(TilesConfiguration::isEnabled)
            .filter(TilesConfiguration::hasDatasetTiles)
            .map(TilesConfiguration::getZoomLevelsDerived)
            .map(Map::keySet)
            .orElse(ImmutableSet.of())
            .stream()
            .collect(Collectors.toUnmodifiableList());

    Set<String> tmsSet =
        apiData.getCollections().values().stream()
            .filter(collection -> apiData.isCollectionEnabled(collection.getId()))
            .map(collection -> collection.getExtension(TilesConfiguration.class))
            .filter(config -> config.filter(ExtensionConfiguration::isEnabled).isPresent())
            .map(config -> config.get().getZoomLevelsDerived().keySet())
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

    tmsSet.addAll(tmsSetMultiCollection);

    return tmsSet.stream()
        .filter(tms -> tileMatrixSetRepository.get(tms).isPresent())
        .collect(ImmutableList.toImmutableList());
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
    return "tileMatrixSetIdTiles";
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
    if (isApplicable(apiData, definitionPath)) return false;

    return apiData
        .getCollections()
        .get(collectionId)
        .getExtension(TilesConfiguration.class)
        .map(ExtensionConfiguration::isEnabled)
        .orElse(true);
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
    return isEnabledForApi(apiData)
        && (definitionPath.startsWith("/collections/{collectionId}/tiles/{tileMatrixSetId}")
            || definitionPath.startsWith("/tiles/{tileMatrixSetId}"));
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TilesConfiguration.class;
  }
}
