/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tiles.domain.provider.TileEncoder;
import no.ecc.vectortile.VectorTileEncoder;

public class TileEncoderMvt implements TileEncoder {

  @Override
  public byte[] empty(TileMatrixSet tms) {
    return new VectorTileEncoder(tms.getTileExtent()).encode();
  }

  @Override
  public byte[] combine(TileMatrixSet tms) {
    return new byte[0];
  }
}
