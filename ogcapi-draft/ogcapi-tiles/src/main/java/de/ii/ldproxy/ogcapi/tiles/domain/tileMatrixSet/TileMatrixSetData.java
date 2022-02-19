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
import de.ii.ldproxy.ogcapi.foundation.domain.Link;
import de.ii.ldproxy.ogcapi.foundation.domain.PageRepresentationWithId;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * This class specifies the data structure of a tile matrix set.
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileMatrixSetData.Builder.class)
public abstract class TileMatrixSetData extends PageRepresentationWithId {

    public abstract List<String> getKeywords();

    public abstract String getCrs();

    public abstract Optional<URI> getWellKnownScaleSet();

    public abstract Optional<URI> getUri();

    public abstract Optional<TilesBoundingBox> getBoundingBox();

    public abstract List<TileMatrix> getTileMatrices();

    public abstract List<String> getOrderedAxes();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<TileMatrixSetData> FUNNEL = (from, into) -> {
        PageRepresentationWithId.FUNNEL.funnel(from, into);
        from.getKeywords()
            .stream()
            .sorted()
            .forEachOrdered(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getLinks()
            .stream()
            .sorted(Comparator.comparing(Link::getHref))
            .forEachOrdered(link -> into.putString(link.getHref(), StandardCharsets.UTF_8)
                                        .putString(Objects.requireNonNullElse(link.getRel(), ""), StandardCharsets.UTF_8));
        into.putString(from.getCrs(), StandardCharsets.UTF_8);
        from.getUri().ifPresent(s -> into.putString(s.toString(), StandardCharsets.UTF_8));
        from.getBoundingBox().ifPresent(val -> TilesBoundingBox.FUNNEL.funnel(val, into));
        from.getTileMatrices()
            .stream()
            .sorted(Comparator.comparing(TileMatrix::getId))
            .forEachOrdered(val -> TileMatrix.FUNNEL.funnel(val, into));
        from.getOrderedAxes()
            .stream()
            .sorted()
            .forEachOrdered(val -> into.putString(val, StandardCharsets.UTF_8));
    };
}
