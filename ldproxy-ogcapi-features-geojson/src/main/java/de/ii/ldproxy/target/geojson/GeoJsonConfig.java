/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import org.immutables.value.Value;

@Value.Immutable
public interface GeoJsonConfig {
    boolean isEnabled();

    FeatureTransformerGeoJson.NESTED_OBJECTS getNestedObjectStrategy();

    FeatureTransformerGeoJson.MULTIPLICITY getMultiplicityStrategy();

    @Value.Default
    default boolean getUseFormattedJsonOutput() { return false; }

    @Value.Default
    default String getSeparator() {
        return ".";
    }
}
