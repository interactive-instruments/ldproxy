/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableMbStyleRasterDemSource.class)
public abstract class MbStyleRasterDemSource extends MbStyleSource {
    public enum Encoding { terrarium, mapbox }

    public final String getType() {
        return "raster-dem";
    }

    public abstract Optional<String> getUrl();

    public abstract Optional<List<String>> getTiles();

    public abstract Optional<List<Double>> getBounds(); // { return Optional.of(ImmutableList.of(-180.0,-85.051129,180.0,85.051129)); }

    public abstract Optional<Integer> getTilesize(); // { return Optional.of(512); }

    public abstract Optional<Integer> getMinzoom(); // { return Optional.of(0); }

    public abstract Optional<Integer> getMaxzoom(); // { return Optional.of(22); }

    public abstract Optional<String> getAttribution();

    @Value.Default
    public Encoding getEncoding() {
        return Encoding.mapbox;
    }
}
