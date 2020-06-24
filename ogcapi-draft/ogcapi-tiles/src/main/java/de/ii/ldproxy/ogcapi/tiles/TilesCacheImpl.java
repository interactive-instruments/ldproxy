/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * Access to the cache for tile files.
 */
@Component
@Provides
@Instantiate
public class TilesCacheImpl implements TilesCache {

    private static Logger LOGGER = LoggerFactory.getLogger(TilesCacheImpl.class);

    private static final String TILES_DIR_NAME = "tiles";
    private static final String TMP_DIR_NAME = "__tmp__";
    private static final long TEN_MINUTES = 10 * 60 * 1000;
    private File dataDirectory;
    private long lastCleanup = System.currentTimeMillis();

    /**
     * set data directory
     */
    public TilesCacheImpl(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) {
        // the ldproxy data directory, in development environment this would be ./build/data
        this.dataDirectory = new File(bundleContext.getProperty(DATA_DIR_KEY));

        // TODO move to background task
        cleanup();
    }

    /**
     * return and if necessary create the directory for the tiles cache
     * @return the file object of the directory
     */
    @Override
    public File getTilesDirectory() {
        File tilesDirectory = new File(dataDirectory, TILES_DIR_NAME);
        if (!tilesDirectory.exists()) {
            tilesDirectory.mkdirs();
        }
        return tilesDirectory;
    }


    /**
     * return and if necessary create the directory for the tile files that cannot be cached
     * @return the file object of the directory
     */
    @Override
    public File getTmpDirectory() {
        File tmpDirectory = new File(new File(dataDirectory, TILES_DIR_NAME), TMP_DIR_NAME);
        if (!tmpDirectory.exists()) {
            tmpDirectory.mkdirs();
        }
        return tmpDirectory;
    }

    /**
     * clean-up files that cannot be cached due to the use of parameters
     */
    @Override
    public void cleanup() {
        Runnable cleanup = () -> {
            File tmpDirectory = getTmpDirectory();
            long cutoff = System.currentTimeMillis() - TEN_MINUTES;
            File[] files = tmpDirectory.listFiles();
            if (files!=null) {
                for (File file : files) {
                    if (file.lastModified() <= cutoff) {
                        file.delete();
                    }
                }
            }
            lastCleanup = System.currentTimeMillis();
        };

        new Thread(cleanup).start();
    }

    /**
     * fetch the file object of a tile in the cache
     *
     * @param tile     the tile
     * @return the file object of the tile in the cache
     */
    @Override
    public File getFile(Tile tile) {
        return new File(getTileDirectory(tile), tile.getFileName());
    }

    /**
     * retrieve the subdirectory in the tiles directory for the selected tile
     *
     * @param tile the tile
     * @return the directory for the cached tile
     */
    private File getTileDirectory(Tile tile) {
        File subDir;
        if (tile.getTemporary()) {
            subDir = getTmpDirectory();
            if (System.currentTimeMillis() - lastCleanup > TEN_MINUTES)
                cleanup();
        } else
            subDir = new File(new File(new File(getTilesDirectory(), tile.getApi().getId()), (tile.getCollectionIds().size()!=1 ? "__all__" : tile.getCollectionId())), tile.getTileMatrixSet().getId());

        if (!subDir.exists()) {
            subDir.mkdirs();
        }
        return subDir;
    }

}
