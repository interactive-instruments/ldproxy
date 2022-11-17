/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import de.ii.xtraplatform.cql.domain.Cql2Expression;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, attributeBuilderDetection = true)
public interface TileQuery extends TileCoordinates {
  String getLayer();

  Optional<UserGenerationParameters> userParameters();

  @Value.Immutable
  interface UserGenerationParameters {

    OptionalInt getLimit();

    List<Cql2Expression> getFilters();

    List<String> getFields();
  }
}
