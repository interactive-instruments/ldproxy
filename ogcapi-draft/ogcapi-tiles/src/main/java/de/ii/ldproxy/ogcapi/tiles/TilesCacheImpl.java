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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static de.ii.ldproxy.ogcapi.domain.FoundationConfiguration.CACHE_DIR;
import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;

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
    private final Path tilesStore;
    private long lastCleanup = System.currentTimeMillis();

    /**
     * set data directory
     */
    public TilesCacheImpl(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) throws IOException {
        // the ldproxy data directory, in development environment this would be ./build/data
        this.tilesStore = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), CACHE_DIR)
                               .resolve(TILES_DIR_NAME);
        if (Files.notExists(tilesStore)) {
            if (Files.notExists(tilesStore.getParent())) {
                Files.createDirectory(tilesStore.getParent());
            }
            Files.createDirectory(tilesStore);
        }

        // TODO move to background task
        cleanup();
    }

    /**
     * return and if necessary create the directory for the tiles cache
     * @return the file object of the directory
     */
    @Override
    public Path getTilesStore() {
        return tilesStore;
    }


    /**
     * return and if necessary create the directory for the tile files that cannot be cached
     * @return the file object of the directory
     */
    @Override
    public Path getTmpDirectory() throws IOException {
        Path tmpDirectory = tilesStore.resolve(TMP_DIR_NAME);
        if (Files.notExists(tmpDirectory)) {
            Files.createDirectory(tmpDirectory);
        }
        return tmpDirectory;
    }

    /**
     * clean-up files that cannot be cached due to the use of parameters
     */
    @Override
    public void cleanup() {
        Runnable cleanup = () -> {
            try {
                Path tmpDirectory = getTmpDirectory();
                long cutoff = FileTime.from(Instant.now()).toMillis() - TEN_MINUTES;
                Files.list(tmpDirectory)
                     .filter(path -> {
                         try {
                             return Files.getLastModifiedTime(path).toMillis() <= cutoff;
                         } catch (IOException e) {
                             throw new RuntimeException("Error while cleaning the tile cache.", e);
                         }
                     })
                     .forEach(path -> {
                         try {
                             Files.delete(path);
                         } catch (IOException e) {
                             throw new RuntimeException("Error while cleaning the tile cache.", e);
                         }
                     });
                lastCleanup = System.currentTimeMillis();
            } catch (IOException e) {
                throw new RuntimeException("Error while cleaning the tile cache.", e);
            }
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
    public Path getFile(Tile tile) throws IOException {
        return getTileDirectory(tile).resolve(tile.getFileName());
    }

    /**
     * retrieve the subdirectory in the tiles directory for the selected tile
     *
     * @param tile the tile
     * @return the directory for the cached tile
     */
    private Path getTileDirectory(Tile tile) throws IOException {
        Path subDir;
        if (tile.getTemporary()) {
            subDir = getTmpDirectory();
            if (FileTime.from(Instant.now()).toMillis() - lastCleanup > TEN_MINUTES)
                cleanup();
        } else {
            subDir = getTilesStore().resolve(tile.getApi().getId())
                                    .resolve(tile.getCollectionIds().size() != 1 ? "__all__" : tile.getCollectionId())
                                    .resolve(tile.getTileMatrixSet().getId());
        }

        if (Files.notExists(subDir)) {
            if (Files.notExists(subDir.getParent())) {
                if (Files.notExists(subDir.getParent().getParent())) {
                    Files.createDirectory(subDir.getParent().getParent());
                }
                Files.createDirectory(subDir.getParent());
            }
            Files.createDirectory(subDir);
        }
        return subDir;
    }

}
