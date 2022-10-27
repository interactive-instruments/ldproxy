/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import de.ii.xtraplatform.features.domain.FeatureStream;
import javax.ws.rs.core.MediaType;

public interface TileGenerator {

  boolean supports(MediaType mediaType);

  byte[] generateTile(TileQuery tileQuery, MediaType mediaType);

  // TODO: TileStream, TileEncoder, TileQuery
  FeatureStream getTileSource(TileQuery tileQuery);
}
