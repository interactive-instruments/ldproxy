/**
 * Copyright 2019 interactive instruments GmbH
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
import de.ii.ldproxy.wfs3.api.ExtensionConfiguration;
import de.ii.ldproxy.wfs3.aroundrelations.AroundRelationsConfiguration;
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
                .put(AroundRelationsConfiguration.class, ExtensionConfiguration.getExtensionType(AroundRelationsConfiguration.class))
                .put(CrsConfiguration.class, ExtensionConfiguration.getExtensionType(CrsConfiguration.class))
                .put(FilterTransformersConfiguration.class, ExtensionConfiguration.getExtensionType(FilterTransformersConfiguration.class))
                .put(GeneralizationConfiguration.class, ExtensionConfiguration.getExtensionType(GeneralizationConfiguration.class))
                .put(GeoJsonConfiguration.class, ExtensionConfiguration.getExtensionType(GeoJsonConfiguration.class))
                .put(GmlConfiguration.class, ExtensionConfiguration.getExtensionType(GmlConfiguration.class))
                .put(HtmlConfiguration.class, ExtensionConfiguration.getExtensionType(HtmlConfiguration.class))
                .put(Oas30Configuration.class, ExtensionConfiguration.getExtensionType(Oas30Configuration.class))
                .put(ProjectionsConfiguration.class, ExtensionConfiguration.getExtensionType(ProjectionsConfiguration.class))
                .put(SitemapsConfiguration.class, ExtensionConfiguration.getExtensionType(SitemapsConfiguration.class))
                .put(TilesConfiguration.class, ExtensionConfiguration.getExtensionType(TilesConfiguration.class))
                .put(TransactionalConfiguration.class, ExtensionConfiguration.getExtensionType(TransactionalConfiguration.class))
                .put(StylesConfiguration.class, ExtensionConfiguration.getExtensionType(StylesConfiguration.class))
                .put(RequestGeoJsonBboxConfiguration.class, RequestGeoJsonBboxConfiguration.TRANSFORMER_TYPE)
                .build();
    }
}
