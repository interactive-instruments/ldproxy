/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import de.ii.ogcapi.common.domain.OgcApiExtent;
import de.ii.ogcapi.foundation.domain.PageRepresentation;
import de.ii.ogcapi.foundation.domain.PageRepresentationWithId;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableOgcApiCollection.Builder.class)
public abstract class OgcApiCollection extends PageRepresentationWithId {

    // Core, part 1
    public abstract Optional<OgcApiExtent> getExtent();
    public abstract Optional<String> getItemType();
    public abstract List<String> getCrs();

    // CRS, part 2
    public abstract Optional<String> getStorageCrs();
    public abstract Optional<Float> getStorageCrsCoordinateEpoch();

    // restrict to information in ogcapi-stable, everything else goes into the extensions map

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<OgcApiCollection> FUNNEL = (from, into) -> {
        PageRepresentation.FUNNEL.funnel(from, into);
        from.getExtent().ifPresent(val -> OgcApiExtent.FUNNEL.funnel(val, into));
        from.getItemType().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getCrs()
            .stream()
            .sorted()
            .forEachOrdered(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getStorageCrs().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getStorageCrsCoordinateEpoch().ifPresent(val -> into.putFloat(val));
        from.getExtensions()
            .keySet()
            .stream()
            .sorted()
            .forEachOrdered(key -> into.putString(key, StandardCharsets.UTF_8));
        // we cannot encode the generic extension object
    };
}
