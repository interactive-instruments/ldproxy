/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Access to the store static tile providers.
 */
public interface StaticTileProviderStore {

    /**
     * return and if necessary create the directory for the static tile provider store
     * @return the file object of the directory
     */
    Path getTileProviderStore();

    /**
     * return a static tile provider
     * @param apiData the API
     * @param filename the filename of the static tile provider
     * @return the file object of the store
     */
    Path getTileProvider(OgcApiDataV2 apiData, String filename);

    /**
     * fetch the file object of a tile in the cache
     *
     * @param tile     the tile
     * @return the file object of the tile in the cache
     */
    InputStream getTile(Path tileProvider, Tile tile);

}
