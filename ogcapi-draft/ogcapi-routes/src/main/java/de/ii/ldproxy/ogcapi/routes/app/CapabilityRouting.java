/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.app;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableHtmlForm;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRoutingConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import static de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration.DefaultCrs.CRS84;

@Singleton
@AutoBind
public class CapabilityRouting implements ApiBuildingBlock {

    public static String CORE = "http://www.opengis.net/spec/ogcapi-routes-1/1.0.0-draft.1/conf/core";
    public static String MODE = "http://www.opengis.net/spec/ogcapi-routes-1/1.0.0-draft.1/conf/mode";
    public static String INTERMEDIATE_WAYPOINTS = "http://www.opengis.net/spec/ogcapi-routes-1/1.0.0-draft.1/conf/intermediate-waypoints";
    public static String HEIGHT = "http://www.opengis.net/spec/ogcapi-routes-1/1.0.0-draft.1/conf/height";
    public static String WEIGHT = "http://www.opengis.net/spec/ogcapi-routes-1/1.0.0-draft.1/conf/weight";
    public static String OBSTACLES = "http://www.opengis.net/spec/ogcapi-routes-1/1.0.0-draft.1/conf/obstacles";
    public static String MANAGE_ROUTES = "http://www.opengis.net/spec/ogcapi-routes-1/1.0.0-draft.1/conf/manage-routes";

    @Inject
    CapabilityRouting() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableRoutingConfiguration.Builder()
            .enabled(false)
            .manageRoutes(false)
            .intermediateWaypoints(false)
            .weightRestrictions(false)
            .heightRestrictions(false)
            .obstacles(false)
            .defaultPreference("fastest")
            .defaultMode("driving")
            .additionalFlags(ImmutableMap.of())
            .defaultCrs(CRS84)
            .html(ImmutableHtmlForm.builder()
                      .enabled(true)
                      .build())
            .build();
    }

}
