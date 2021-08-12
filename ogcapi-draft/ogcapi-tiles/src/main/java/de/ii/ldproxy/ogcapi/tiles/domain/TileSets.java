/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import de.ii.ldproxy.ogcapi.collections.domain.Collections;
import de.ii.ldproxy.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ldproxy.ogcapi.domain.PageRepresentation;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileSets.Builder.class)
public abstract class TileSets extends PageRepresentation {

    public abstract List<TileSet> getTilesets();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<TileSets> FUNNEL = (from, into) -> {
        PageRepresentation.FUNNEL.funnel(from, into);
        from.getTilesets()
            .stream()
            .sorted(Comparator.comparing(TileSet::getTileMatrixSetId))
            .forEachOrdered(val -> TileSet.FUNNEL.funnel(val, into));
        from.getExtensions()
            .keySet()
            .stream()
            .sorted()
            .forEachOrdered(key -> into.putString(key, StandardCharsets.UTF_8));
        // we cannot encode the generic extension object
    };
}
