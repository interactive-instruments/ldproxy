/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.app;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class CesiumData3dTiles {

  public final boolean clampToGround;
  public final Optional<String> ionAccessToken;
  public final Optional<String> maptilerApiKey;
  public final Optional<String> customTerrainProviderUri;
  public final String encodedTileset;

  public CesiumData3dTiles(
      String tilesetJson,
      boolean clampToGround,
      Optional<String> ionAccessToken,
      Optional<String> maptilerApiKey,
      Optional<String> customTerrainProviderUri) {
    this.ionAccessToken = ionAccessToken;
    this.maptilerApiKey = maptilerApiKey;
    this.customTerrainProviderUri = customTerrainProviderUri;
    this.clampToGround =
        clampToGround
            || (ionAccessToken.isEmpty()
                && maptilerApiKey.isEmpty()
                && customTerrainProviderUri.isEmpty());
    this.encodedTileset = URLEncoder.encode(tilesetJson, StandardCharsets.UTF_8);
  }

  public boolean fromTiles() {
    return true;
  }
}
