/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import de.ii.ldproxy.ogcapi.foundation.domain.PageRepresentation;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSet;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSets;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileMatrixSets.Builder.class)
public abstract class TileMatrixSets extends PageRepresentation {

    public abstract List<TileMatrixSetLinks> getTileMatrixSets();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<TileMatrixSets> FUNNEL = (from, into) -> {
        PageRepresentation.FUNNEL.funnel(from, into);
        from.getTileMatrixSets()
            .stream()
            .sorted(Comparator.comparing(TileMatrixSetLinks::getId))
            .forEachOrdered(val -> TileMatrixSetLinks.FUNNEL.funnel(val, into));
        from.getExtensions()
            .keySet()
            .stream()
            .sorted()
            .forEachOrdered(key -> into.putString(key, StandardCharsets.UTF_8));
        // we cannot encode the generic extension object
    };
}
