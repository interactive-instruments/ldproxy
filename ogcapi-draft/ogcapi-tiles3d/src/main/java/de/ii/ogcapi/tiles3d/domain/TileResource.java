/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.domain;

import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.immutables.value.Value;

/** This class represents a resource in a 3D Tiles tileset with implicit tiling */
@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true)
public abstract class TileResource {

  public enum TYPE {
    CONTENT,
    SUBTREE
  }

  /**
   * @return the level of the tile
   */
  public abstract int getLevel();

  /**
   * @return the column of the tile
   */
  public abstract int getX();

  /**
   * @return the row of the tile
   */
  public abstract int getY();

  /**
   * @return the id of the collection included in the tile
   */
  public abstract String getCollectionId();

  /**
   * @return the API that produces the tile
   */
  public abstract OgcApi getApi();

  /**
   * @return the API that produces the tile
   */
  @Value.Derived
  @Value.Auxiliary
  public OgcApiDataV2 getApiData() {
    return getApi().getData();
  }

  /**
   * @return the resource type
   */
  public abstract TYPE getType();

  @Value.Derived
  @Value.Auxiliary
  public Path getRelativePath() {
    String extension = getType() == TYPE.CONTENT ? "glb" : "subtree";
    return Paths.get(String.format("%d_%d_%d.%s", getLevel(), getX(), getY(), extension));
  }
}
