/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import static de.ii.ogcapi.foundation.domain.FoundationConfiguration.API_RESOURCES_DIR;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles.app.TilesBuildingBlock;
import de.ii.ogcapi.tiles.app.mbtiles.MbtilesMetadata;
import de.ii.ogcapi.tiles.app.mbtiles.MbtilesMetadata.MbtilesFormat;
import de.ii.ogcapi.tiles.app.mbtiles.MbtilesTileset;
import de.ii.ogcapi.tiles.domain.StaticTileProviderStore;
import de.ii.ogcapi.tiles.domain.Tile;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.provider.TileProviderMbtilesData;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;

/** Access tiles in Mbtiles files. */
@Singleton
@AutoBind
public class StaticTileProviderStoreImpl implements StaticTileProviderStore {

  private static final String TILES_DIR_NAME = "tiles";
  private final Path store;
  private Map<String, MbtilesTileset> mbtiles;

  @Inject
  public StaticTileProviderStoreImpl(AppContext appContext) {
    this.store = appContext.getDataDir().resolve(API_RESOURCES_DIR).resolve(TILES_DIR_NAME);
    this.mbtiles = new HashMap<>();
  }

  /**
   * check that all tile set container exist and register them all in a map
   *
   * @param api the API
   * @param apiValidation the validation level
   * @return the validation result
   */
  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().mode(apiValidation);

    try {
      Files.createDirectories(store);
    } catch (IOException e) {
      builder.addErrors(e.getMessage());
    }

    Optional<TilesConfiguration> config = api.getData().getExtension(TilesConfiguration.class);
    if (config.isPresent()
        && config.get().isEnabled()
        && config.get().isMultiCollectionEnabled()
        && config.get().getTileProvider() instanceof TileProviderMbtilesData) {
      TileProviderMbtilesData provider = (TileProviderMbtilesData) config.get().getTileProvider();
      Path path = getTileProvider(api.getData(), provider.getFilename());
      String key = String.join("/", api.getId(), TilesBuildingBlock.DATASET_TILES);
      try {
        mbtiles.put(key, new MbtilesTileset(path));
      } catch (Exception e) {
        builder.addErrors(
            MessageFormat.format(
                "The Mbtiles container for the multi-collection tile provider at path ''{0}'' could not be initialized.",
                path.toString()));
      }
    }

    for (String collectionId : api.getData().getCollections().keySet()) {
      config = api.getData().getExtension(TilesConfiguration.class, collectionId);
      if (config.isPresent()
          && config.get().isEnabled()
          && config.get().isSingleCollectionEnabled()
          && config.get().getTileProvider() instanceof TileProviderMbtilesData) {
        TileProviderMbtilesData provider = (TileProviderMbtilesData) config.get().getTileProvider();
        Path path = getTileProvider(api.getData(), provider.getFilename());
        String key = String.join("/", api.getId(), collectionId);
        try {
          mbtiles.put(key, new MbtilesTileset(path));
        } catch (Exception e) {
          builder.addErrors(
              MessageFormat.format(
                  "The Mbtiles container for the tile provider for collection ''{1}'' at path ''{0}'' could not be initialized.",
                  path.toString(), collectionId));
        }
      }
    }

    return builder.build();
  }

  @Override
  public Path getTileProviderStore() {
    return store;
  }

  @Override
  public Path getTileProvider(OgcApiDataV2 apiData, String filename) {
    return store.resolve(apiData.getId()).resolve(filename);
  }

  @Override
  public InputStream getTile(Path tileProvider, Tile tile) {
    String key =
        String.join(
            "/",
            tile.getApiData().getId(),
            tile.isDatasetTile() ? TilesBuildingBlock.DATASET_TILES : tile.getCollectionId());
    MbtilesTileset tileset = mbtiles.get(key);
    try {
      return tileset.getTile(tile).orElseThrow(NotFoundException::new);
    } catch (SQLException | IOException e) {
      throw new RuntimeException(
          String.format(
              "Error accessing tile %d/%d/%d in dataset '%s' in Mbtiles file '%s', format '%s'.",
              tile.getTileLevel(),
              tile.getTileRow(),
              tile.getTileCol(),
              tile.getApiData().getId(),
              tileProvider.toString(),
              tile.getOutputFormat().getExtension()),
          e);
    }
  }

  @Override
  public Optional<Integer> getMinzoom(OgcApiDataV2 apiData, String filename)
      throws SQLException, IOException {
    MbtilesTileset tileset = new MbtilesTileset(getTileProvider(apiData, filename));
    return tileset.getMetadata().getMinzoom();
  }

  @Override
  public Optional<Integer> getMaxzoom(OgcApiDataV2 apiData, String filename)
      throws SQLException, IOException {
    MbtilesTileset tileset = new MbtilesTileset(getTileProvider(apiData, filename));
    return tileset.getMetadata().getMaxzoom();
  }

  @Override
  public Optional<Integer> getDefaultzoom(OgcApiDataV2 apiData, String filename)
      throws SQLException, IOException {
    MbtilesTileset tileset = new MbtilesTileset(getTileProvider(apiData, filename));
    List<Number> center = tileset.getMetadata().getCenter();
    if (center.size() == 3) return Optional.of(Math.round(center.get(2).floatValue()));
    return Optional.empty();
  }

  @Override
  public List<Double> getCenter(OgcApiDataV2 apiData, String filename)
      throws SQLException, IOException {
    MbtilesTileset tileset = new MbtilesTileset(getTileProvider(apiData, filename));
    List<Number> center = tileset.getMetadata().getCenter();
    if (center.size() >= 2)
      return ImmutableList.of(center.get(0).doubleValue(), center.get(1).doubleValue());
    return ImmutableList.of();
  }

  @Override
  public String getFormat(OgcApiDataV2 apiData, String filename) throws SQLException, IOException {
    MbtilesTileset tileset = new MbtilesTileset(getTileProvider(apiData, filename));
    MbtilesFormat format = tileset.getMetadata().getFormat();
    if (format == MbtilesMetadata.MbtilesFormat.pbf) return "MVT";
    else if (format == MbtilesMetadata.MbtilesFormat.jpg) return "JPEG";
    else if (format == MbtilesMetadata.MbtilesFormat.png) return "PNG";
    else if (format == MbtilesMetadata.MbtilesFormat.webp) return "WEBP";
    else if (format == MbtilesMetadata.MbtilesFormat.tiff) return "TIFF";

    throw new UnsupportedOperationException(
        String.format("Mbtiles format '%s' is currently not supported.", format));
  }
}
