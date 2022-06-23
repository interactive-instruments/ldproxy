/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.json.fg.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableWhereConfiguration.Builder.class)
public interface WhereConfiguration {

  @Nullable
  Boolean getEnabled();

  @Nullable
  Boolean getAlwaysIncludeGeoJsonGeometry();

  default WhereConfiguration mergeInto(WhereConfiguration src) {
    if (Objects.isNull(src)) return this;

    ImmutableWhereConfiguration.Builder builder =
        new ImmutableWhereConfiguration.Builder().from(src).from(this);

    return builder.build();
  }
}
