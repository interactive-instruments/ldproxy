/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableOgcApiCollection.Builder.class)
public abstract class OgcApiCollection extends PageRepresentationWithId {

    // restrict to information in ogcapi-stable, everything else goes into the extensions map

    // Core, part 1
    public abstract Optional<OgcApiExtent> getExtent();
    public abstract Optional<String> getItemType();
    public abstract List<String> getCrs();

    // CRS, part 2
    public abstract Optional<String> getStorageCrs();
    public abstract Optional<Float> getStorageCrsCoordinateEpoch();

    @Value.Derived
    public Optional<String> getNativeCrs() { return getStorageCrs(); }

    @Value.Derived
    public Optional<Float> getNativeCrsCoordinateEpoch() { return getStorageCrsCoordinateEpoch(); }

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();
}
