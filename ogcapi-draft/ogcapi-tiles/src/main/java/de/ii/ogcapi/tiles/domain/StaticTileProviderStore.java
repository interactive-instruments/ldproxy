/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import de.ii.ogcapi.foundation.domain.ApiExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;

import java.io.InputStream;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Access to the store static tile providers.
 */
public interface StaticTileProviderStore extends ApiExtension {

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

    /**
     * fetch the minzoom value from the metadata in the tile set container
     * @param apiData the API
     * @param filename the filename of the tile set container
     * @return the minzoom value
     */
    Optional<Integer> getMinzoom(OgcApiDataV2 apiData, String filename) throws SQLException;

    /**
     * fetch the maxzoom value from the metadata in the tile set container
     * @param apiData the API
     * @param filename the filename of the tile set container
     * @return the maxzoom value
     */
    Optional<Integer> getMaxzoom(OgcApiDataV2 apiData, String filename) throws SQLException;

    /**
     * fetch the zoom level of the default view from the metadata in the tile set container
     * @param apiData the API
     * @param filename the filename of the tile set container
     * @return the zoom level
     */
    Optional<Integer> getDefaultzoom(OgcApiDataV2 apiData, String filename) throws SQLException;

    /**
     * fetch the location of the default view from the metadata in the tile set container
     * @param apiData the API
     * @param filename the filename of the tile set container
     * @return the location as longitude, latitude
     */
    List<Double> getCenter(OgcApiDataV2 apiData, String filename) throws SQLException;

    /**
     * fetch the format of the tiles from the metadata in the tile set container
     * @param apiData the API
     * @param filename the filename of the tile set container
     * @return the format
     */
    String getFormat(OgcApiDataV2 apiData, String filename) throws SQLException;
}
