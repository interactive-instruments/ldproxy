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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@AutoMultiBind
public interface TypedQueryParameter<T> {

  String getName();

  default int getPriority() {
    // default is to parse in first pass
    return 1;
  }

  T parse(
      String value,
      Map<String, Object> typedValues,
      OgcApi api,
      Optional<FeatureTypeConfigurationOgcApi> optionalCollectionData);

  default T parse(
      List<String> value,
      Map<String, Object> typedValues,
      OgcApi api,
      Optional<FeatureTypeConfigurationOgcApi> optionalCollectionData) {
    if (Objects.isNull(value) || value.isEmpty()) {
      return null;
    }
    return parse(value.get(0), typedValues, api, optionalCollectionData);
  }
}
