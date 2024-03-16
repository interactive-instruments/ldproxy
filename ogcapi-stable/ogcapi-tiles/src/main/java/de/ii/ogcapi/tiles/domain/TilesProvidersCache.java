/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.xtraplatform.base.domain.resiliency.Volatile2;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.util.Optional;

public interface TilesProvidersCache extends Volatile2 {
  void deleteTiles(
      OgcApi api,
      Optional<String> collectionId,
      Optional<String> tileMatrixSetId,
      Optional<BoundingBox> boundingBox);
}
