/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import java.io.File;

/**
 * Access to the cache for tile files.
 */
public interface TilesCache {

    /**
     * return and if necessary create the directory for the tiles cache
     * @return the file object of the directory
     */
    File getTilesDirectory();


    /**
     * return and if necessary create the directory for the tile files that cannot be cached
     * @return the file object of the directory
     */
    File getTmpDirectory();

    /**
     * clean-up files that cannot be cached due to the use of parameters
     */
    void cleanup();

    /**
     * fetch the file object of a tile in the cache
     *
     * @param tile     the tile
     * @return the file object of the tile in the cache
     */
    File getFile(Tile tile);

}
