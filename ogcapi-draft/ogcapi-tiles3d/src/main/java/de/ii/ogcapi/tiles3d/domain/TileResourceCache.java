/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.domain;

import de.ii.ogcapi.foundation.domain.ApiExtension;
import de.ii.ogcapi.foundation.domain.OgcApi;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/** Access to the cache for tile files. */
public interface TileResourceCache extends ApiExtension {

  /**
   * check whether a tile resource is cached
   *
   * @param r the tile resource
   * @return {@code true}, if the resource is included in the cache
   * @throws IOException an error occurred while accessing files
   */
  boolean tileResourceExists(TileResource r) throws IOException;

  /**
   * fetch a tile resource from the cache
   *
   * @param r the tile resource
   * @return the tile as an input stream; the result is empty, if the tile is not cached
   * @throws IOException an error occurred while accessing files
   */
  Optional<InputStream> getTileResource(TileResource r) throws IOException;

  /**
   * store a tile resource in the cache
   *
   * @param r the tile resource
   * @param content the byte array of the file content
   * @throws IOException an error occurred while accessing files
   */
  void storeTileResource(TileResource r, byte[] content) throws IOException;

  /**
   * delete a tile from the cache
   *
   * @param r the tile resource
   * @throws IOException an error occurred while accessing files
   */
  void deleteTileResource(TileResource r) throws IOException;

  /**
   * get a file handle for a resource
   *
   * @param r the tile resource
   * @throws IOException an error occurred while accessing files
   */
  File getFile(TileResource r) throws IOException;

  /**
   * delete the cached resources for the API
   *
   * @param api
   * @throws IOException an error occurred while accessing files
   */
  void deleteTiles(OgcApi api) throws IOException;
}
