/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import static de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;
import static de.ii.ogcapi.tiles.domain.PathParameterTileMatrixSetId.TMS_REGEX;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import java.util.Optional;

@AutoMultiBind
public interface TileSetFormatExtension extends GenericFormatExtension {

  @Override
  default String getPathPattern() {
    return "^(?:/collections/" + COLLECTION_ID_PATTERN + ")?(?:/map)?/tiles/" + TMS_REGEX + "/?$";
  }

  Object getTileSetEntity(
      TileSet tileset,
      OgcApiDataV2 apiData,
      Optional<String> collectionId,
      ApiRequestContext requestContext);
}
