/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface LayerOptionsFeaturesDefault extends LayerOptionsCommonDefault {
  Optional<String> getFeatureProvider();
}
