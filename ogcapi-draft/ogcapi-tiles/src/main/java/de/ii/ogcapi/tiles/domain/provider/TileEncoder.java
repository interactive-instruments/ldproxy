/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import java.io.IOException;

public interface TileEncoder {

  byte[] empty(TileMatrixSet tms);

  byte[] combine(TileQuery tile, TileProviderFeaturesData data, ChainedTileProvider tileProvider)
      throws IOException;
}
