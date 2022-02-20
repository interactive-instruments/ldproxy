/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableMbStyleGeojsonSource.class)
public abstract class MbStyleGeojsonSource extends MbStyleSource {
    public final String getType() {
        return "geojson";
    }

    public abstract Optional<Object> getData();

    @Value.Default
    public Integer getMaxzoom() {
        return 18;
    }

    public abstract Optional<String> getAttribution();

    public abstract Optional<Integer> getBuffer(); // { return Optional.of(128); }

    public abstract Optional<Double> getTolerance(); // { return Optional.of(0.375); }

    public abstract Optional<Boolean> getCluster(); // { return Optional.of(false); }

    public abstract Optional<Integer> getClusterRadius(); // { return Optional.of(50); }

    public abstract Optional<Integer> getClusterMaxZoom(); // { return Optional.of(Integer.valueOf(getMaxzoom()-1)); }

    public abstract Optional<Object> getClusterProperties();

    public abstract Optional<Boolean> getLineMetrics(); // { return Optional.of(false); }

    public abstract Optional<Boolean> getGenerateId(); // { return Optional.of(false); }
}
