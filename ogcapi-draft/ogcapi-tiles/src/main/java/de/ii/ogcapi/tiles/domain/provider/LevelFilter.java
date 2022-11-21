/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.domain.Cql2Expression;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableLevelFilter.Builder.class)
public interface LevelFilter extends WithLevels {

  String getFilter();

  @JsonIgnore
  @Nullable
  Cql2Expression getCqlFilter();

  @Value.Derived
  default boolean isParsed() {
    return Objects.nonNull(getCqlFilter());
  }
}
