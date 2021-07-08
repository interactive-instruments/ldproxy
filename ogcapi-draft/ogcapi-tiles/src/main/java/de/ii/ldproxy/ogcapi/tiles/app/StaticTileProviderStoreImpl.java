/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.tiles.app.mbtiles.MbtilesMetadata;
import de.ii.ldproxy.ogcapi.tiles.app.mbtiles.MbtilesTileset;
import de.ii.ldproxy.ogcapi.tiles.domain.StaticTileProviderStore;
import de.ii.ldproxy.ogcapi.tiles.domain.Tile;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.ii.ldproxy.ogcapi.domain.FoundationConfiguration.API_RESOURCES_DIR;
import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;

/**
 * Access tiles in Mbtiles files.
 */
@Component
@Provides
@Instantiate
public class StaticTileProviderStoreImpl implements StaticTileProviderStore {

    private static final String TILES_DIR_NAME = "tiles";
    private final Path store;
    private Map<String, MbtilesTileset> mbtiles;

    public StaticTileProviderStoreImpl(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) throws IOException {
        this.store = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), API_RESOURCES_DIR)
                                            .resolve(TILES_DIR_NAME);
        Files.createDirectories(store);
        mbtiles = new HashMap<>();
    }

    /**
     * check that all tile set container exist and register them all in a map
     * @param apiData the API
     * @param apiValidation the validation level
     * @return the validation result
     */
    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, ValidationResult.MODE apiValidation) {
        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
                                                                             .mode(apiValidation);

        Optional<TilesConfiguration> config = apiData.getExtension(TilesConfiguration.class);
        if (config.isPresent()
                && config.get().isEnabled()
                && config.get().getMultiCollectionEnabledDerived()
                && config.get().getTileProvider() instanceof TileProviderMbtiles) {
            TileProviderMbtiles provider = (TileProviderMbtiles) config.get().getTileProvider();
            Path path = getTileProvider(apiData, provider.getFilename());
            String key = String.join("/", apiData.getId(), CapabilityVectorTiles.DATASET_TILES);
            try {
                mbtiles.put(key, new MbtilesTileset(path));
            } catch (Exception e) {
                builder.addErrors(MessageFormat.format("The Mbtiles container for the multi-collection tile provider at path ''{0}'' could not be initialized.", path.toString()));
            }
        }

        for (String collectionId : apiData.getCollections().keySet()) {
            config = apiData.getExtension(TilesConfiguration.class, collectionId);
            if (config.isPresent()
                    && config.get().isEnabled()
                    && config.get().getSingleCollectionEnabledDerived()
                    && config.get().getTileProvider() instanceof TileProviderMbtiles) {
                TileProviderMbtiles provider = (TileProviderMbtiles) config.get().getTileProvider();
                Path path = getTileProvider(apiData, provider.getFilename());
                String key = String.join("/", apiData.getId(), collectionId);
                try {
                    mbtiles.put(key, new MbtilesTileset(path));
                } catch (Exception e) {
                    builder.addErrors(MessageFormat.format("The Mbtiles container for the tile provider for collection ''{1}'' at path ''{0}'' could not be initialized.", path.toString(), collectionId));
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
        String key = String.join("/", tile.getApiData().getId(), tile.isDatasetTile() ? CapabilityVectorTiles.DATASET_TILES : tile.getCollectionId());
        MbtilesTileset tileset = mbtiles.get(key);
        try {
            return tileset.getTile(tile).orElseThrow(() -> new javax.ws.rs.NotFoundException());
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error accessing tile %d/%d/%d in dataset '%s' in Mbtiles file '%s', format '%s'.",
                                                     tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(),
                                                     tile.getApiData().getId(), tileProvider.toString(), tile.getOutputFormat().getExtension()), e);
        }
    }

    @Override
    public Optional<Integer> getMinzoom(OgcApiDataV2 apiData, String filename) throws SQLException {
        MbtilesTileset tileset = new MbtilesTileset(getTileProvider(apiData, filename));
        return tileset.getMetadata().getMinzoom();
    }

    @Override
    public Optional<Integer> getMaxzoom(OgcApiDataV2 apiData, String filename) throws SQLException {
        MbtilesTileset tileset = new MbtilesTileset(getTileProvider(apiData, filename));
        return tileset.getMetadata().getMaxzoom();
    }

    @Override
    public Optional<Integer> getDefaultzoom(OgcApiDataV2 apiData, String filename) throws SQLException {
        MbtilesTileset tileset = new MbtilesTileset(getTileProvider(apiData, filename));
        List<Number> center = tileset.getMetadata().getCenter();
        if (center.size()==3)
            return Optional.of(Math.round(center.get(2).floatValue()));
        return  Optional.empty();
    }

    @Override
    public List<Double> getCenter(OgcApiDataV2 apiData, String filename) throws SQLException {
        MbtilesTileset tileset = new MbtilesTileset(getTileProvider(apiData, filename));
        List<Number> center = tileset.getMetadata().getCenter();
        if (center.size()>=2)
            return ImmutableList.of(center.get(0).doubleValue(), center.get(1).doubleValue());
        return null;
    }

    @Override
    public String getFormat(OgcApiDataV2 apiData, String filename) throws SQLException {
        MbtilesTileset tileset = new MbtilesTileset(getTileProvider(apiData, filename));
        MbtilesMetadata.MbtilesFormat format = tileset.getMetadata().getFormat();
        if (format==MbtilesMetadata.MbtilesFormat.pbf)
            return "MVT";

        // TODO support bitmap formats
        throw new UnsupportedOperationException(String.format("Mbtiles format '%s' is currently not supported.", format));
    }
}
