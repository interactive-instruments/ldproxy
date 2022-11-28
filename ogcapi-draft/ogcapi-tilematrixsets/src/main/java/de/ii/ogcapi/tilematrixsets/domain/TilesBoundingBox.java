/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tilematrixsets.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTilesBoundingBox.Builder.class)
public abstract class TilesBoundingBox {

  public abstract BigDecimal[] getLowerLeft();

  public abstract BigDecimal[] getUpperRight();

  public abstract Optional<String> getCrs();

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<TilesBoundingBox> FUNNEL =
      (from, into) -> {
        Arrays.stream(from.getLowerLeft()).forEachOrdered(val -> into.putDouble(val.doubleValue()));
        Arrays.stream(from.getUpperRight())
            .forEachOrdered(val -> into.putDouble(val.doubleValue()));
        from.getCrs().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
      };
}
