/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableMbStyleStylesheet.class)
public abstract class MbStyleStylesheet {

    public enum Visibility { visible, none }

    public final int getVersion() { return 8; }
    public abstract Optional<String> getName();
    public abstract Optional<Object> getMetadata();
    public abstract Optional<List<Double>> getCenter();
    public abstract Optional<Double> getZoom();
    @Value.Default
    public Double getBearing() { return 0.0; }
    @Value.Default
    public Double getPitch() { return 0.0; }
    public abstract Optional<MbStyleLight> getLight();
    public abstract Map<String, MbStyleSource> getSources();
    public abstract Optional<String> getSprite();
    public abstract Optional<String> getGlyphs();
    public abstract Optional<MbStyleTransition> getTransition();
    public abstract List<MbStyleLayer> getLayers();

}
