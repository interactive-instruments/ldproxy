/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.api.ExtensionConfiguration;
import de.ii.xtraplatform.dropwizard.api.JacksonSubTypeIds;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Map;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class JacksonSubTypeIdsGeoJson implements JacksonSubTypeIds {
    @Override
    public Map<Class<?>, String> getMapping() {
        return new ImmutableMap.Builder<Class<?>, String>()
                .put(GeoJsonPropertyMapping.class, "GEO_JSON_PROPERTY")
                .put(GeoJsonGeometryMapping.class, "GEO_JSON_GEOMETRY")
                .put(GeoJsonConfiguration.class, ExtensionConfiguration.getExtensionType(GeoJsonConfiguration.class))
                .build();
    }
}
