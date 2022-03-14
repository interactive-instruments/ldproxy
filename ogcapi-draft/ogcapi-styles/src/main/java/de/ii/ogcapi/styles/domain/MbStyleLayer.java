/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableMbStyleLayer.class)
public abstract class MbStyleLayer {
    public enum LayerType {
        background("background"),
        fill("fill"),
        line("line"),
        symbol("symbol"),
        raster("raster"),
        circle("circle"),
        fillExtrusion("fill-extrusion"),
        heatmap("heatmap"),
        hillshade("hillshade");

        public final String label;

        LayerType(String label) {
            this.label = label;
        }

        @Override
        public String toString() { return label; }
    }

    public abstract String getId();

    // TODO use enum to check that we have a valid value
    public abstract String getType();

    public abstract Map<String, Object> getMetadata();

    public abstract Optional<String> getSource();

    @JsonProperty("source-layer")
    public abstract Optional<String> getSourceLayer();

    public abstract Optional<Integer> getMinzoom();

    public abstract Optional<Integer> getMaxzoom();

    public abstract Optional<Object> getFilter();

    public abstract Map<String, Object> getLayout();

    public abstract Map<String, Object> getPaint();
}
