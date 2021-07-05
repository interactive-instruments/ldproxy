/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.tiles.domain.StaticTileProviderStore;
import de.ii.ldproxy.ogcapi.tiles.domain.Tile;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.imintel.mbtiles4j.MBTilesReadException;
import org.imintel.mbtiles4j.MBTilesReader;
import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

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

    public StaticTileProviderStoreImpl(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) throws IOException {
        this.store = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), API_RESOURCES_DIR)
                                            .resolve(TILES_DIR_NAME);
        Files.createDirectories(store);
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
        int level = tile.getTileLevel();
        int row = tile.getTileRow();
        int col = tile.getTileCol();
        GZIPInputStream inputStream;
        try {
            MBTilesReader r = new MBTilesReader(tileProvider.toFile());
            row = tile.getTileMatrixSet().getRows(level) - 1 - row;
            inputStream = new GZIPInputStream(r.getTile(level, col, row).getData());
            r.close();
        } catch (MBTilesReadException | IOException e) {
            throw new RuntimeException("Error accessing a Mbtiles tile.", e);
        }

        return inputStream;
    }
}
