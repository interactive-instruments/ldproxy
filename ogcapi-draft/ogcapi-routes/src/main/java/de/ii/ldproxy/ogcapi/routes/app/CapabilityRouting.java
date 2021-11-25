/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.app;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableHtmlForm;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRoutingConfiguration;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRoutingFlag;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import static de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration.DefaultCrs.CRS84;

@Component
@Provides
@Instantiate
public class CapabilityRouting implements ApiBuildingBlock {

    public static String CORE = "http://www.opengis.net/spec/ogcapi-routes-1/0.0/conf/core";
    public static String INTERMEDIATE_WAYPOINTS = "http://www.opengis.net/spec/ogcapi-routes-1/0.0/conf/intermediate-waypoints";

    private final ExtensionRegistry extensionRegistry;

    public CapabilityRouting(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableRoutingConfiguration.Builder()
            .enabled(false)
            //.async(false) TODO not yet implemented
            //.callback(false) TODO not yet implemented
            .intermediateWaypoints(false)
            .preferences(ImmutableMap.of("fastest",new ImmutableRoutingFlag.Builder().label("fastest").build()))
            .defaultPreference("fastest")
            .additionalFlags(ImmutableMap.of())
            .defaultCrs(CRS84)
            .html(ImmutableHtmlForm.builder()
                      .enabled(true)
                      .build())
            .build();
    }

}
