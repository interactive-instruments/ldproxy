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
import de.ii.ogcapi.foundation.domain.ApiInfo;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.PageRepresentationWithId;
import de.ii.xtraplatform.tiles.domain.TileMatrix;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetData;
import de.ii.xtraplatform.tiles.domain.TilesBoundingBox;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

/** This class specifies the data structure of a tile matrix set. */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableTileMatrixSetOgcApi.Builder.class)
@ApiInfo(schemaId = "TileMatrixSet")
public abstract class TileMatrixSetOgcApi extends PageRepresentationWithId
    implements TileMatrixSetData {

  public static TileMatrixSetOgcApi of(TileMatrixSetData tileMatrixSetData) {
    return new ImmutableTileMatrixSetOgcApi.Builder().from(tileMatrixSetData).build();
  }

  public static final String SCHEMA_REF = "#/components/schemas/TileMatrixSet";

  public abstract List<String> getKeywords();

  public abstract String getCrs();

  public abstract Optional<URI> getWellKnownScaleSet();

  public abstract Optional<URI> getUri();

  public abstract Optional<TilesBoundingBox> getBoundingBox();

  public abstract List<TileMatrix> getTileMatrices();

  public abstract List<String> getOrderedAxes();

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<TilesBoundingBox> FUNNEL_BBOX =
      (from, into) -> {
        Arrays.stream(from.getLowerLeft()).forEachOrdered(val -> into.putDouble(val.doubleValue()));
        Arrays.stream(from.getUpperRight())
            .forEachOrdered(val -> into.putDouble(val.doubleValue()));
        from.getCrs().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
      };

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<TileMatrix> FUNNEL_MATRIX =
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

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<TileMatrixSetOgcApi> FUNNEL =
      (from, into) -> {
        PageRepresentationWithId.FUNNEL.funnel(from, into);
        from.getKeywords().stream()
            .sorted()
            .forEachOrdered(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getLinks().stream()
            .sorted(Comparator.comparing(Link::getHref))
            .forEachOrdered(
                link ->
                    into.putString(link.getHref(), StandardCharsets.UTF_8)
                        .putString(
                            Objects.requireNonNullElse(link.getRel(), ""), StandardCharsets.UTF_8));
        into.putString(from.getCrs(), StandardCharsets.UTF_8);
        from.getUri().ifPresent(s -> into.putString(s.toString(), StandardCharsets.UTF_8));
        from.getBoundingBox().ifPresent(val -> FUNNEL_BBOX.funnel(val, into));
        from.getTileMatrices().stream()
            .sorted(Comparator.comparing(TileMatrix::getId))
            .forEachOrdered(val -> FUNNEL_MATRIX.funnel(val, into));
        from.getOrderedAxes().stream()
            .sorted()
            .forEachOrdered(val -> into.putString(val, StandardCharsets.UTF_8));
      };
}
