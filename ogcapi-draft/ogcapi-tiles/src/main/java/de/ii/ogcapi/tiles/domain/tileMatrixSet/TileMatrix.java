/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.tileMatrixSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileMatrix.Builder.class)
public abstract class TileMatrix {

  public static final int SIGNIFICANT_DIGITS = 15;

  public abstract String getId();

  public abstract Optional<String> getTitle();

  public abstract Optional<String> getDescription();

  public abstract List<String> getKeywords();

  public abstract long getTileWidth();

  public abstract long getTileHeight();

  public abstract long getMatrixWidth();

  public abstract long getMatrixHeight();

  public abstract BigDecimal getScaleDenominator();

  public abstract BigDecimal getCellSize();

  public abstract BigDecimal[] getPointOfOrigin();

  @Value.Default
  public String getCornerOfOrigin() {
    return "topLeft";
  }

  @JsonIgnore
  @Value.Derived
  public int getTileLevel() {
    return Integer.parseInt(getId());
  }

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<TileMatrix> FUNNEL =
      (from, into) -> {
        into.putString(from.getId(), StandardCharsets.UTF_8);
        from.getTitle().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getDescription().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getKeywords().stream()
            .sorted()
            .forEachOrdered(val -> into.putString(val, StandardCharsets.UTF_8));
        into.putLong(from.getTileWidth());
        into.putLong(from.getTileHeight());
        into.putLong(from.getMatrixWidth());
        into.putLong(from.getMatrixHeight());
        into.putDouble(from.getScaleDenominator().doubleValue());
        Arrays.stream(from.getPointOfOrigin())
            .forEachOrdered(val -> into.putDouble(val.doubleValue()));
        into.putString(from.getCornerOfOrigin(), StandardCharsets.UTF_8);
      };
}
