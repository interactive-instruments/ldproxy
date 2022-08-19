/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.domain;

import static de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import java.util.List;

@AutoMultiBind
public interface Format3dTilesSubtree extends GenericFormatExtension {

  default String getPathPattern() {
    return "^/?collections/" + COLLECTION_ID_PATTERN + "/3dtiles/subtree/[\\d+]/[\\d+]/[\\d+]/?$";
  }

  Object getEntity(
      Object subtree,
      List<Link> links,
      String collectionId,
      OgcApi api,
      ApiRequestContext requestContext);
}
