/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.base.Strings;
import de.ii.xtraplatform.cfgstore.api.BundleConfigDefault;
import de.ii.xtraplatform.cfgstore.api.ConfigPropertyDescriptor;
import de.ii.xtraplatform.cfgstore.api.handler.LocalBundleConfig;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import static de.ii.ldproxy.target.geojson.GeoJsonConfigImpl.*;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@LocalBundleConfig(bundleId = "ldproxy-target-geojson", category = "GeoJson Output Format", properties = {
        @ConfigPropertyDescriptor(name = ENABLED, label = "Enable GeoJson output format?", defaultValue = "true", uiType = ConfigPropertyDescriptor.UI_TYPE.CHECKBOX),
        @ConfigPropertyDescriptor(name = NESTED_OBJECTS, label = "How to format nested objects?", defaultValue = "NEST", uiType = ConfigPropertyDescriptor.UI_TYPE.SELECT, allowedValues = "{NEST: 'Nest', FLATTEN: 'Flatten'}"),
        @ConfigPropertyDescriptor(name = MULTIPLICITY, label = "How to format multiple values?", defaultValue = "ARRAY", uiType = ConfigPropertyDescriptor.UI_TYPE.SELECT, allowedValues = "{ARRAY: 'Array', SUFFIX: 'Suffix'}"),
        @ConfigPropertyDescriptor(name = FORMATTEDOUTPUT, label = "Pretty print the JSON?", defaultValue = "false", uiType = ConfigPropertyDescriptor.UI_TYPE.CHECKBOX)
})
public class GeoJsonConfigImpl extends BundleConfigDefault implements GeoJsonConfig {

    static final String ENABLED = "enabled";
    static final String NESTED_OBJECTS = "nestedObjects";
    static final String MULTIPLICITY = "multiplicity";
    static final String FORMATTEDOUTPUT = "useFormattedJsonOutput";

    @Override
    public boolean isEnabled() {
        return Strings.nullToEmpty(properties.get(ENABLED))
                      .toLowerCase()
                      .equals("true");
    }

    @Override
    public FeatureTransformerGeoJson.NESTED_OBJECTS getNestedObjectStrategy() {
        FeatureTransformerGeoJson.NESTED_OBJECTS nestedObjects;
        try {
            nestedObjects = FeatureTransformerGeoJson.NESTED_OBJECTS.valueOf(Strings.nullToEmpty(properties.get(NESTED_OBJECTS)));
        } catch (Throwable e) {
            nestedObjects = FeatureTransformerGeoJson.NESTED_OBJECTS.NEST;
        }

        return nestedObjects;
    }

    @Override
    public FeatureTransformerGeoJson.MULTIPLICITY getMultiplicityStrategy() {
        FeatureTransformerGeoJson.MULTIPLICITY multiplicity;
        try {
            multiplicity = FeatureTransformerGeoJson.MULTIPLICITY.valueOf(Strings.nullToEmpty(properties.get(MULTIPLICITY)));
        } catch (Throwable e) {
            multiplicity = FeatureTransformerGeoJson.MULTIPLICITY.ARRAY;
        }

        return multiplicity;
    }

    @Override
    public boolean getUseFormattedJsonOutput() {
        return Strings.nullToEmpty(properties.get(FORMATTEDOUTPUT))
                .toLowerCase()
                .equals("true");
    }
}
