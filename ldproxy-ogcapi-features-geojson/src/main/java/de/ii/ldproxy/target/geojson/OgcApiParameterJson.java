/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiParameterExtension;
import de.ii.xtraplatform.runtime.FelixRuntime;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class OgcApiParameterJson implements OgcApiParameterExtension {

    private final boolean allowDebug;

    public OgcApiParameterJson(@Context BundleContext context) {
        this.allowDebug = FelixRuntime.ENV.valueOf(context.getProperty(FelixRuntime.ENV_KEY)) == FelixRuntime.ENV.DEVELOPMENT;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, GeoJsonConfiguration.class);
    }

    @Override
    public ImmutableSet<String> getParameters(OgcApiDatasetData apiData, String subPath) {
        if (!isEnabledForApi(apiData))
            return ImmutableSet.of();

        if (subPath.matches("^/[\\w\\-]+/items/?$")) {
            // Features
            return allowDebug ? ImmutableSet.of("pretty", "debug") : ImmutableSet.of("pretty");
        } else if (subPath.matches("^/[\\w\\-]+/items/[^/\\s]+/?$")) {
            // Feature
            return allowDebug ? ImmutableSet.of("pretty", "debug") : ImmutableSet.of("pretty");
        }

        return ImmutableSet.of();
    }
}
