/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.cityjson.domain;

import de.ii.ogcapi.features.core.domain.EncodingAwareContext;
import org.immutables.value.Value;

import java.util.Objects;

@Value.Modifiable
public interface EncodingAwareContextCityJson extends EncodingAwareContext<FeatureTransformationContextCityJson> {

  @Value.Derived
  @Value.Auxiliary
  default ModifiableStateCityJson getState() {
    return Objects.requireNonNull(encoding().getState());
  }
}
