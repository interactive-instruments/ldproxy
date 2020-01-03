/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Information about the cache for vector tile files.
 */
public class VectorTilesCache {

    private static Logger LOGGER = LoggerFactory.getLogger(VectorTilesCache.class);

    private static final String TILES_DIR_NAME = "tiles";
    private static final String TMP_DIR_NAME = "__tmp__";
    private static final long TEN_MINUTES = 10 * 60 * 1000;
    private File dataDirectory;

    /**
     * set data directory
     * @param dataDirectory the ldproxy data directory, in development environment this would be ./build/data
     */
    VectorTilesCache(String dataDirectory) {
        this.dataDirectory = new File(dataDirectory);
        cleanup();
    }

    /**
     * return and if necessary create the directory for the tiles cache
     * @return the file object of the directory
     */
    File getTilesDirectory() {
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
    File getTmpDirectory() {
        File tmpDirectory = new File(new File(dataDirectory, TILES_DIR_NAME), TMP_DIR_NAME);
        if (!tmpDirectory.exists()) {
            tmpDirectory.mkdirs();
        }
        return tmpDirectory;
    }

    /**
     * clean-up files that cannot be cached due to the use of parameters
     */
    void cleanup() {
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
        };

        new Thread(cleanup).start();
    }
}
