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
import de.ii.ldproxy.ogcapi.foundation.domain.PageRepresentationWithId;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileMatrixSetLinks.Builder.class)
public abstract class TileMatrixSetLinks extends PageRepresentationWithId {

    public abstract Optional<String> getTileMatrixSetURI();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<TileMatrixSetLinks> FUNNEL = (from, into) -> {
        PageRepresentationWithId.FUNNEL.funnel(from, into);
        from.getTileMatrixSetURI().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
    };
}
