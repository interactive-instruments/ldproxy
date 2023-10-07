/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import static de.ii.ogcapi.tiles.app.TilesBuildingBlock.DATASET_TILES;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ExtendableConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimitsGenerator;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimitsOgcApi;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesProviders;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.entities.domain.EntityRegistry;
import de.ii.xtraplatform.tiles.domain.MinMax;
import de.ii.xtraplatform.tiles.domain.TileMatrixSet;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetRepository;
import de.ii.xtraplatform.tiles.domain.TileProvider;
import de.ii.xtraplatform.tiles.domain.TilesetMetadata;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class TilesProvidersImpl implements TilesProviders {

  private static final Logger LOGGER = LoggerFactory.getLogger(TilesProvidersImpl.class);

  private final EntityRegistry entityRegistry;
  private final TileMatrixSetLimitsGenerator limitsGenerator;
  private final TileMatrixSetRepository tileMatrixSetRepository;

  @Inject
  public TilesProvidersImpl(
      EntityRegistry entityRegistry,
      TileMatrixSetLimitsGenerator limitsGenerator,
      TileMatrixSetRepository tileMatrixSetRepository) {
    this.entityRegistry = entityRegistry;
    this.limitsGenerator = limitsGenerator;
    this.tileMatrixSetRepository = tileMatrixSetRepository;
  }

  @Override
  public boolean hasTileProvider(OgcApiDataV2 apiData) {
    return getTileProvider(apiData).isPresent();
  }

  @Override
  public Optional<TileProvider> getTileProvider(OgcApiDataV2 apiData) {
    Optional<TileProvider> optionalTileProvider = getOptionalTileProvider(apiData);

    if (!optionalTileProvider.isPresent()) {
      optionalTileProvider =
          entityRegistry.getEntity(TileProvider.class, TilesProviders.toTilesId(apiData.getId()));
    }
    return optionalTileProvider;
  }

  @Override
  public TileProvider getTileProviderOrThrow(OgcApiDataV2 apiData) {
    return getTileProvider(apiData)
        .orElseThrow(() -> new IllegalStateException("No tile provider found."));
  }

  @Override
  public boolean hasTileProvider(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData) {
    return getTileProvider(apiData, collectionData).isPresent();
  }

  @Override
  public Optional<TileProvider> getTileProvider(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData) {
    return getOptionalTileProvider(collectionData).or(() -> getTileProvider(apiData));
  }

  @Override
  public TileProvider getTileProviderOrThrow(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData) {
    return getOptionalTileProvider(collectionData).orElse(getTileProviderOrThrow(apiData));
  }

  private Optional<TileProvider> getOptionalTileProvider(
      ExtendableConfiguration extendableConfiguration) {
    return extendableConfiguration
        .getExtension(TilesConfiguration.class)
        .filter(ExtensionConfiguration::isEnabled)
        .flatMap(cfg -> Optional.ofNullable(cfg.getTileProviderId()))
        .flatMap(id -> entityRegistry.getEntity(TileProvider.class, id));
  }

  @Override
  public void deleteTiles(
      OgcApi api,
      Optional<String> collectionId,
      Optional<String> tileMatrixSetId,
      Optional<BoundingBox> boundingBox) {
    LOGGER.info(
        "Purging tile cache for collection '{}', tiling scheme '{}', bounding box '{}'",
        collectionId.orElse("*"),
        tileMatrixSetId.orElse("*"),
        boundingBox.isEmpty()
            ? "*"
            : String.format(
                Locale.US,
                "%f,%f,%f,%f",
                boundingBox.get().getXmin(),
                boundingBox.get().getYmin(),
                boundingBox.get().getXmax(),
                boundingBox.get().getYmax()));

    OgcApiDataV2 apiData = api.getData();
    Optional<TilesetMetadata> tilesetMetadata =
        getTilesetMetadata(apiData, collectionId.flatMap(apiData::getCollectionData));
    if (tilesetMetadata.isEmpty()) return;

    Map<String, MinMax> zoomLevels = tilesetMetadata.get().getLevels();

    Map<String, MinMax> relevantZoomLevels =
        tileMatrixSetId
            .map(
                tmsId ->
                    zoomLevels.entrySet().stream()
                        .filter(entry -> Objects.equals(tmsId, entry.getKey()))
                        .collect(
                            Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(zoomLevels);

    Map<String, BoundingBox> relevantBoundingBoxes =
        relevantZoomLevels.keySet().stream()
            .map(
                tmsId -> {
                  TileMatrixSet tileMatrixSet = tileMatrixSetRepository.get(tmsId).orElseThrow();
                  BoundingBox bbox =
                      boundingBox.orElseGet(
                          () ->
                              api.getSpatialExtent(collectionId)
                                  .orElse(tileMatrixSet.getBoundingBox()));
                  return new SimpleImmutableEntry<>(tmsId, bbox);
                })
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

    deleteTiles(apiData, collectionId, relevantZoomLevels, relevantBoundingBoxes);

    LOGGER.info("Purging tile cache has finished");
  }

  private void deleteTiles(
      OgcApiDataV2 apiData,
      Optional<String> collectionId,
      Map<String, MinMax> zoomLevels,
      Map<String, BoundingBox> boundingBoxes) {
    for (Map.Entry<String, MinMax> tileSet : zoomLevels.entrySet()) {
      TileMatrixSet tileMatrixSet = tileMatrixSetRepository.get(tileSet.getKey()).orElseThrow();
      MinMax levels = tileSet.getValue();
      BoundingBox bbox = boundingBoxes.get(tileSet.getKey());

      // first the dataset tiles
      deleteTiles(apiData, Optional.empty(), tileMatrixSet, levels, bbox);

      if (collectionId.isPresent()) {
        // also the single collection tiles for the collection
        deleteTiles(apiData, collectionId, tileMatrixSet, levels, bbox);
      } else {
        // all single collection tiles
        for (String colId : apiData.getCollections().keySet()) {
          deleteTiles(apiData, Optional.of(colId), tileMatrixSet, levels, bbox);
        }
      }
    }
  }

  private void deleteTiles(
      OgcApiDataV2 apiData,
      Optional<String> collectionId,
      TileMatrixSet tileMatrixSet,
      MinMax levels,
      BoundingBox bbox) {
    List<TileMatrixSetLimitsOgcApi> limitsList =
        limitsGenerator.getTileMatrixSetLimits(bbox, tileMatrixSet, levels);
    for (TileMatrixSetLimitsOgcApi limits : limitsList) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "Deleting tiles from cache: API {}, collection {}, tiles {}/{}/{}-{}/{}-{}, TMS rows {}-{}",
            apiData.getId(),
            collectionId.orElse(DATASET_TILES),
            tileMatrixSet.getId(),
            limits.getTileMatrix(),
            limits.getMinTileRow(),
            limits.getMaxTileRow(),
            limits.getMinTileCol(),
            limits.getMaxTileCol(),
            tileMatrixSet.getTmsRow(
                Integer.parseInt(limits.getTileMatrix()), limits.getMaxTileRow()),
            tileMatrixSet.getTmsRow(
                Integer.parseInt(limits.getTileMatrix()), limits.getMinTileRow()));
      }

      getTileProviderOrThrow(apiData, collectionId.flatMap(apiData::getCollectionData))
          .deleteFromCache(collectionId.orElse(DATASET_TILES), tileMatrixSet, limits);
    }
  }
}
