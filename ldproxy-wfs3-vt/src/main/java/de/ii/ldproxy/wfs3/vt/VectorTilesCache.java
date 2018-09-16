package de.ii.ldproxy.wfs3.vt;

import javax.ws.rs.NotFoundException;
import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.Set;

/**
 * Information about the cache for vector tile files.
 *
 * @author portele
 */
public class VectorTilesCache {

    private static final String TILES_DIR_NAME = "tiles";
    private static final String TILING_SCHEMES_DIR_NAME = "tilingSchemes";
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
     * return and if necessary create the directory for the tiles cache
     * @return the file object of the directory
     */
    private File getTilingSchemesDirectory() {
        File tilingSchemesDirectory = new File(dataDirectory, TILING_SCHEMES_DIR_NAME);
        if (!tilingSchemesDirectory.exists()) {
            tilingSchemesDirectory.mkdirs();
        }
        return tilingSchemesDirectory;
    }

    /**
     * return tiling scheme by id
     * @return the file object of the tiling scheme
     */
    File getTilingScheme(String tilingSchemeId) {
        File tilingSchemesDirectory = getTilingSchemesDirectory();
        File file = new File(tilingSchemesDirectory, tilingSchemeId + ".json");
        if (!file.exists())
            throw new NotFoundException();
        return file;
    }

    /**
     * fetch set of tiling scheme identifiers supported by the service
     * @return the set of tiling scheme identifiers
     */
    Set<String> getTilingSchemeIds() {
        Set<String> schemes = new HashSet<>();
        File tilingSchemesDirectory = getTilingSchemesDirectory();
        File[] files = tilingSchemesDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().toLowerCase().endsWith(".json");
            }
        });
        if (files!=null) {
            for (File file : files) {
                // remove extension ".json"
                String filename = file.getName();
                filename = filename.substring(0, filename.lastIndexOf("."));
                schemes.add(filename);
            }
        }
        return schemes;
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
