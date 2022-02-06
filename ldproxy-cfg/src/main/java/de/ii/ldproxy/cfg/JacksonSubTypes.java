/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.ldproxy.ogcapi.collections.app.JacksonSubTypeIdsOgcApiCollections;
import de.ii.ldproxy.ogcapi.collections.queryables.domain.JacksonSubTypeIdsQueryables;
import de.ii.ldproxy.ogcapi.collections.schema.app.JacksonSubTypeIdsSchema;
import de.ii.ldproxy.ogcapi.common.domain.JacksonSubTypeIdsOgcApiCommon;
import de.ii.ldproxy.ogcapi.crs.app.JacksonSubTypeIdsCrs;
import de.ii.ldproxy.ogcapi.domain.JacksonSubTypeIdsFoundation;
import de.ii.ldproxy.ogcapi.features.core.app.JacksonSubTypeIdsFeaturesCore;
import de.ii.ldproxy.ogcapi.features.extensions.app.JacksonSubTypeIdsFeaturesExtensions;
import de.ii.ldproxy.ogcapi.features.geojson.app.JacksonSubTypeIdsGeoJson;
import de.ii.ldproxy.ogcapi.features.geojsonld.app.JacksonSubTypeIdsGeoJsonLd;
import de.ii.ldproxy.ogcapi.features.gml.app.JacksonSubTypeIdsGml;
import de.ii.ldproxy.ogcapi.features.html.app.JacksonSubTypeIdsFeaturesHtml;
import de.ii.ldproxy.ogcapi.features.jsonfg.app.JacksonSubTypeIdsJsonFg;
import de.ii.ldproxy.ogcapi.features.transactional.JacksonSubTypeIdsTransactional;
import de.ii.ldproxy.ogcapi.filter.domain.JacksonSubTypeIdsFilter;
import de.ii.ldproxy.ogcapi.geometry_simplification.JacksonSubTypeIdsGeometrySimplification;
import de.ii.ldproxy.ogcapi.html.app.JacksonSubTypeIdsHtml;
import de.ii.ldproxy.ogcapi.json.app.JacksonSubTypeIdsJson;
import de.ii.ldproxy.ogcapi.maps.app.JacksonSubTypeIdsMapTiles;
import de.ii.ldproxy.ogcapi.oas30.app.JacksonSubTypeIdsOas30;
import de.ii.ldproxy.ogcapi.projections.JacksonSubTypeIdsProjections;
import de.ii.ldproxy.ogcapi.sorting.JacksonSubTypeIdsSorting;
import de.ii.ldproxy.ogcapi.styles.app.JacksonSubTypeIdsStyles;
import de.ii.ldproxy.ogcapi.tiles.app.JacksonSubTypeIdsTiles;
import de.ii.ldproxy.ogcapi.xml.app.JacksonSubTypeIdsXml;
import de.ii.ldproxy.resources.app.JacksonSubTypeIdsResources;
import de.ii.xtraplatform.dropwizard.domain.JacksonSubTypeIds;
import de.ii.xtraplatform.feature.provider.pgis.FeatureProviderRegisterPgis;
import de.ii.xtraplatform.feature.provider.wfs.FeatureProviderRegisterWfs;
import java.util.ArrayList;
import java.util.List;

public interface JacksonSubTypes {

  static List<JacksonSubTypeIds> ids() {
    List<JacksonSubTypeIds> ids = new ArrayList<>();

    ids.add(new JacksonSubTypeIdsQueryables());
    ids.add(new JacksonSubTypeIdsSchema());
    ids.add(new JacksonSubTypeIdsFeaturesExtensions());
    ids.add(new JacksonSubTypeIdsGeoJsonLd());
    ids.add(new JacksonSubTypeIdsJsonFg());
    ids.add(new JacksonSubTypeIdsFilter());
    ids.add(new JacksonSubTypeIdsGeometrySimplification());
    ids.add(new JacksonSubTypeIdsMapTiles());
    ids.add(new JacksonSubTypeIdsProjections());
    ids.add(new JacksonSubTypeIdsResources());
    ids.add(new JacksonSubTypeIdsSorting());
    ids.add(new JacksonSubTypeIdsStyles());
    ids.add(new JacksonSubTypeIdsTiles());
    ids.add(new JacksonSubTypeIdsTransactional());
    ids.add(new JacksonSubTypeIdsOgcApiCollections());
    ids.add(new JacksonSubTypeIdsOgcApiCommon());
    ids.add(new JacksonSubTypeIdsCrs());
    ids.add(new JacksonSubTypeIdsFeaturesCore());
    ids.add(new JacksonSubTypeIdsGeoJson());
    ids.add(new JacksonSubTypeIdsGml());
    ids.add(new JacksonSubTypeIdsFeaturesHtml());
    ids.add(new JacksonSubTypeIdsFoundation());
    ids.add(new JacksonSubTypeIdsHtml());
    ids.add(new JacksonSubTypeIdsJson());
    ids.add(new JacksonSubTypeIdsOas30());
    ids.add(new JacksonSubTypeIdsXml());
    ids.add(new FeatureProviderRegisterPgis());
    ids.add(new FeatureProviderRegisterWfs());

    return ids;
  }
}
