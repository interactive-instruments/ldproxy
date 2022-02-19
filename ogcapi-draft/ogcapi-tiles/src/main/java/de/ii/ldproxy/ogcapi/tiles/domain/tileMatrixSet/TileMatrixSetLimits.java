/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableTileMatrixSetLimits.Builder.class)
public abstract class TileMatrixSetLimits {
    public abstract String getTileMatrix();
    public abstract Integer getMinTileRow();
    public abstract Integer getMaxTileRow();
    public abstract Integer getMinTileCol();
    public abstract Integer getMaxTileCol();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<TileMatrixSetLimits> FUNNEL = (from, into) -> {
        into.putString(from.getTileMatrix(), StandardCharsets.UTF_8);
        into.putInt(from.getMinTileRow());
        into.putInt(from.getMaxTileRow());
        into.putInt(from.getMinTileCol());
        into.putInt(from.getMaxTileCol());
    };
}
