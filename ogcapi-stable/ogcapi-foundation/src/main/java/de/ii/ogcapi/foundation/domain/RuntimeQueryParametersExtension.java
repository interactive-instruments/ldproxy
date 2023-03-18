/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import java.util.List;
import java.util.Optional;

@AutoMultiBind
public interface RuntimeQueryParametersExtension extends ApiExtension {

  default List<OgcApiQueryParameter> getRuntimeParameters(
      OgcApiDataV2 apiData, Optional<String> collectionId, String definitionPath) {
    return getRuntimeParameters(apiData, collectionId, definitionPath, HttpMethods.GET);
  }

  List<OgcApiQueryParameter> getRuntimeParameters(
      OgcApiDataV2 apiData,
      Optional<String> collectionId,
      String definitionPath,
      HttpMethods method);
}
