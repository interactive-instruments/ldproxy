/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.domain;

import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.util.List;
import java.util.Map;

public interface Metadata3dSchemaCache {

  GltfSchema getSchema(
      FeatureSchema featureSchema,
      OgcApiDataV2 apiData,
      String collectionId,
      Map<String, GltfProperty> properties,
      List<Codelist> codelists,
      boolean withSurfaceTypes);
}