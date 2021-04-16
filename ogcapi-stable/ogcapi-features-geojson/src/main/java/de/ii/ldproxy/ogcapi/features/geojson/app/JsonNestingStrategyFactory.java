/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformerGeoJson;

public class JsonNestingStrategyFactory {

    public static JsonNestingStrategy getNestingStrategy(FeatureTransformerGeoJson.NESTED_OBJECTS nestedObjects, FeatureTransformerGeoJson.MULTIPLICITY multiplicity, String separator) {

        if (nestedObjects == FeatureTransformerGeoJson.NESTED_OBJECTS.FLATTEN && multiplicity == FeatureTransformerGeoJson.MULTIPLICITY.SUFFIX) {
            return new JsonNestingStrategyFlattenSuffix(separator);
        }

        return new JsonNestingStrategyNestArray();
    }
}
