/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.jsonld;

import com.google.common.collect.ImmutableMap;

import de.ii.ldproxy.target.geojson.GeoJsonConfiguration;
import de.ii.ldproxy.target.gml.GmlConfiguration;
import de.ii.ldproxy.target.html.HtmlConfiguration;
import de.ii.ldproxy.wfs3.aroundrelations.AroundRelationConfiguration;
import de.ii.ldproxy.wfs3.crs.CrsConfiguration;
import de.ii.ldproxy.wfs3.filtertransformer.FilterTransformersConfiguration;
import de.ii.ldproxy.wfs3.filtertransformer.RequestGeoJsonBboxConfiguration;
import de.ii.ldproxy.wfs3.generalization.GeneralizationConfiguration;
import de.ii.ldproxy.wfs3.oas30.Oas30Configuration;
import de.ii.ldproxy.wfs3.projections.ProjectionsConfiguration;
import de.ii.ldproxy.wfs3.sitemaps.SitemapsConfiguration;
import de.ii.ldproxy.wfs3.transactional.TransactionalConfiguration;
import de.ii.ldproxy.wfs3.vt.TilesConfiguration;
import de.ii.ldproxy.wfs3.styles.StylesConfiguration;
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
                .put(CrsConfiguration.class, CrsConfiguration.EXTENSION_TYPE)
                .put(FilterTransformersConfiguration.class, FilterTransformersConfiguration.EXTENSION_TYPE)
                .put(GeneralizationConfiguration.class, GeneralizationConfiguration.EXTENSION_TYPE)
                .put(GeoJsonConfiguration.class, GeoJsonConfiguration.EXTENSION_TYPE)
                .put(GmlConfiguration.class, GmlConfiguration.EXTENSION_TYPE)
                .put(HtmlConfiguration.class, HtmlConfiguration.EXTENSION_TYPE)
                .put(Oas30Configuration.class, Oas30Configuration.EXTENSION_TYPE)
                .put(ProjectionsConfiguration.class, ProjectionsConfiguration.EXTENSION_TYPE)
                .put(RequestGeoJsonBboxConfiguration.class, RequestGeoJsonBboxConfiguration.TRANSFORMER_TYPE)
                .put(SitemapsConfiguration.class, SitemapsConfiguration.EXTENSION_TYPE)
                .put(TilesConfiguration.class, TilesConfiguration.EXTENSION_TYPE)
                .put(TransactionalConfiguration.class, TransactionalConfiguration.EXTENSION_TYPE)

                .put(StylesConfiguration.class, StylesConfiguration.EXTENSION_TYPE)
                .build();
    }
}
