/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TilesBoundingBox;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableTilePoint.Builder.class)
public
interface TilePoint {
    List<Double> getCoordinates();
    Optional<String> getTileMatrix();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<TilePoint> FUNNEL = (from, into) -> {
        from.getCoordinates()
            .forEach(into::putDouble);
        from.getTileMatrix().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
    };
}
