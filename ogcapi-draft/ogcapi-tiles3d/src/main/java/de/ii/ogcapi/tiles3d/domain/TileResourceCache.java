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
import de.ii.xtraplatform.base.domain.resiliency.Volatile2;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

// TODO upgrade to the new cache logic used by TILES

/** Access to the cache for tile files. */
public interface TileResourceCache extends ApiExtension, Volatile2 {

  /**
   * check whether a tile resource is cached
   *
   * @param r the tile resource
   * @return {@code true}, if the resource is included in the cache
   * @throws IOException an error occurred while accessing files
   */
  boolean tileResourceExists(TileResourceDescriptor r) throws IOException;

  /**
   * fetch a tile resource from the cache
   *
   * @param r the tile resource
   * @return the tile as an input stream; the result is empty, if the tile is not cached
   * @throws IOException an error occurred while accessing files
   */
  Optional<InputStream> getTileResource(TileResourceDescriptor r) throws IOException;

  /**
   * store a tile resource in the cache
   *
   * @param r the tile resource
   * @param content the byte array of the file content
   * @throws IOException an error occurred while accessing files
   */
  void storeTileResource(TileResourceDescriptor r, byte[] content) throws IOException;

  /**
   * delete a tile from the cache
   *
   * @param r the tile resource
   * @throws IOException an error occurred while accessing files
   */
  void deleteTileResource(TileResourceDescriptor r) throws IOException;

  /**
   * delete the cached resources for the API
   *
   * @param api
   * @throws IOException an error occurred while accessing files
   */
  void deleteTileResources(OgcApi api) throws IOException;
}
