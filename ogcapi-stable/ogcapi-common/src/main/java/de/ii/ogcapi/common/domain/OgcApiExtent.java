/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import de.ii.ogcapi.foundation.domain.TemporalExtent;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableOgcApiExtent.Builder.class)
public interface OgcApiExtent {

    Optional<OgcApiExtentSpatial> getSpatial();
    Optional<OgcApiExtentTemporal> getTemporal();

    static Optional<OgcApiExtent> of(Optional<BoundingBox> spatial, Optional<TemporalExtent> temporal) {
        if (spatial.isEmpty() && temporal.isEmpty()) {
            return Optional.empty();
        }

        ImmutableOgcApiExtent.Builder builder = ImmutableOgcApiExtent.builder();
        spatial.ifPresent(bbox -> builder.spatial(OgcApiExtentSpatial.of(bbox)));
        temporal.ifPresent(interval -> builder.temporal(OgcApiExtentTemporal.of(interval)));
        return Optional.of(builder.build());
    }

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<OgcApiExtent> FUNNEL = (from, into) -> {
        from.getSpatial().ifPresent(val -> OgcApiExtentSpatial.FUNNEL.funnel(val, into));
        from.getTemporal().ifPresent(val -> OgcApiExtentTemporal.FUNNEL.funnel(val, into));
    };

}
