/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.jsonld;

import com.google.common.collect.ImmutableMap;

import de.ii.ldproxy.wfs3.aroundrelations.AroundRelationConfiguration;
import de.ii.ldproxy.wfs3.filtertransformer.FilterTransformersConfiguration;
import de.ii.ldproxy.wfs3.filtertransformer.RequestGeoJsonBboxConfiguration;
import de.ii.ldproxy.wfs3.vt.TilesConfiguration;

import de.ii.xsf.dropwizard.api.JacksonSubTypeIds;
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
//TODO: moved here because around-relations is not in list for earlier startup
public class TODOAroundRelationsSubtypeIds implements JacksonSubTypeIds {
    @Override
    public Map<Class<?>, String> getMapping() {
        return new ImmutableMap.Builder<Class<?>, String>()
                .put(AroundRelationConfiguration.class, AroundRelationConfiguration.EXTENSION_TYPE)
                .put(RequestGeoJsonBboxConfiguration.class, RequestGeoJsonBboxConfiguration.TRANSFORMER_TYPE)
                .put(FilterTransformersConfiguration.class, FilterTransformersConfiguration.EXTENSION_TYPE)
                .put(TilesConfiguration.class, "TILES")
                .build();
    }
}
