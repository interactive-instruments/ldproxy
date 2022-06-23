/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.collections.app.JacksonSubTypeIdsOgcApiCollections;
import de.ii.ogcapi.collections.queryables.app.JacksonSubTypeIdsQueryables;
import de.ii.ogcapi.collections.schema.app.JacksonSubTypeIdsSchema;
import de.ii.ogcapi.common.domain.JacksonSubTypeIdsOgcApiCommon;
import de.ii.ogcapi.crs.app.JacksonSubTypeIdsCrs;
import de.ii.ogcapi.features.core.app.JacksonSubTypeIdsFeaturesCore;
import de.ii.ogcapi.features.custom.extensions.app.JacksonSubTypeIdsFeaturesExtensions;
import de.ii.ogcapi.features.flatgeobuf.app.JacksonSubTypeIdsFlatgeobuf;
import de.ii.ogcapi.features.geojson.app.JacksonSubTypeIdsGeoJson;
import de.ii.ogcapi.features.geojson.ld.app.JacksonSubTypeIdsGeoJsonLd;
import de.ii.ogcapi.features.gml.app.JacksonSubTypeIdsGml;
import de.ii.ogcapi.features.html.app.JacksonSubTypeIdsFeaturesHtml;
import de.ii.ogcapi.features.json.fg.app.JacksonSubTypeIdsJsonFg;
import de.ii.ogcapi.filter.domain.JacksonSubTypeIdsFilter;
import de.ii.ogcapi.foundation.domain.JacksonSubTypeIdsFoundation;
import de.ii.ogcapi.geometry.simplification.app.JacksonSubTypeIdsGeometrySimplification;
import de.ii.ogcapi.html.app.JacksonSubTypeIdsHtml;
import de.ii.ogcapi.json.app.JacksonSubTypeIdsJson;
import de.ii.ogcapi.maps.app.JacksonSubTypeIdsMapTiles;
import de.ii.ogcapi.oas30.app.JacksonSubTypeIdsOas30;
import de.ii.ogcapi.projections.app.JacksonSubTypeIdsProjections;
import de.ii.ogcapi.resources.app.JacksonSubTypeIdsResources;
import de.ii.ogcapi.sorting.app.JacksonSubTypeIdsSorting;
import de.ii.ogcapi.styles.app.JacksonSubTypeIdsStyles;
import de.ii.ogcapi.tiles.app.JacksonSubTypeIdsTiles;
import de.ii.ogcapi.transactional.app.JacksonSubTypeIdsTransactional;
import de.ii.ogcapi.xml.app.JacksonSubTypeIdsXml;
import de.ii.xtraplatform.base.domain.JacksonSubTypeIds;
import de.ii.xtraplatform.features.sql.app.FeatureProviderRegisterSql;
import java.util.Set;

public interface JacksonSubTypes {

  static Set<JacksonSubTypeIds> ids() {
    return ImmutableSet.<JacksonSubTypeIds>builder()
        .add(new JacksonSubTypeIdsQueryables())
        .add(new JacksonSubTypeIdsSchema())
        .add(new JacksonSubTypeIdsFeaturesExtensions())
        .add(new JacksonSubTypeIdsFlatgeobuf())
        .add(new JacksonSubTypeIdsGeoJsonLd())
        .add(new JacksonSubTypeIdsJsonFg())
        .add(new JacksonSubTypeIdsFilter())
        .add(new JacksonSubTypeIdsGeometrySimplification())
        .add(new JacksonSubTypeIdsMapTiles())
        .add(new JacksonSubTypeIdsProjections())
        .add(new JacksonSubTypeIdsResources())
        .add(new JacksonSubTypeIdsSorting())
        .add(new JacksonSubTypeIdsStyles())
        .add(new JacksonSubTypeIdsTiles())
        .add(new JacksonSubTypeIdsTransactional())
        .add(new JacksonSubTypeIdsOgcApiCollections())
        .add(new JacksonSubTypeIdsOgcApiCommon())
        .add(new JacksonSubTypeIdsCrs())
        .add(new JacksonSubTypeIdsFeaturesCore())
        .add(new JacksonSubTypeIdsGeoJson())
        .add(new JacksonSubTypeIdsGml())
        .add(new JacksonSubTypeIdsFeaturesHtml())
        .add(new JacksonSubTypeIdsFoundation())
        .add(new JacksonSubTypeIdsHtml())
        .add(new JacksonSubTypeIdsJson())
        .add(new JacksonSubTypeIdsOas30())
        .add(new JacksonSubTypeIdsXml())
        .add(new FeatureProviderRegisterSql())
        // .add(new FeatureProviderRegisterWfs())
        .build();
  }
}
