/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.ldproxy.ogcapi.app.ExtensionRegistryImpl;
import de.ii.ldproxy.ogcapi.collections.app.CapabilityCollections;
import de.ii.ldproxy.ogcapi.collections.queryables.app.CapabilityQueryables;
import de.ii.ldproxy.ogcapi.collections.schema.app.CapabilitySchema;
import de.ii.ldproxy.ogcapi.common.domain.CapabilityCommon;
import de.ii.ldproxy.ogcapi.crs.app.CapabilityCrs;
import de.ii.ldproxy.ogcapi.domain.CapabilityFoundation;
import de.ii.ldproxy.ogcapi.features.core.app.CapabilityFeaturesCore;
import de.ii.ldproxy.ogcapi.features.extensions.app.CapabilityFeaturesExtensions;
import de.ii.ldproxy.ogcapi.features.geojson.app.CapabilityGeoJson;
import de.ii.ldproxy.ogcapi.features.geojsonld.app.CapabilityGeoJsonLd;
import de.ii.ldproxy.ogcapi.features.gml.app.CapabilityGml;
import de.ii.ldproxy.ogcapi.features.html.app.CapabilityFeaturesHtml;
import de.ii.ldproxy.ogcapi.features.jsonfg.app.CapabilityJsonFg;
import de.ii.ldproxy.ogcapi.features.transactional.CapabilityTransactional;
import de.ii.ldproxy.ogcapi.filter.app.CapabilityFilter;
import de.ii.ldproxy.ogcapi.geometry_simplification.CapabilityGeometrySimplification;
import de.ii.ldproxy.ogcapi.html.app.CapabilityHtml;
import de.ii.ldproxy.ogcapi.json.app.CapabilityJson;
import de.ii.ldproxy.ogcapi.maps.app.CapabilityMapTiles;
import de.ii.ldproxy.ogcapi.oas30.app.CapabilityOpenApi;
import de.ii.ldproxy.ogcapi.projections.CapabilityProjections;
import de.ii.ldproxy.ogcapi.sorting.CapabilitySorting;
import de.ii.ldproxy.ogcapi.styles.app.CapabilityStyles;
import de.ii.ldproxy.ogcapi.tiles.app.CapabilityTiles;
import de.ii.ldproxy.ogcapi.xml.app.CapabilityXml;
import de.ii.ldproxy.resources.app.CapabilityResources;

class OgcApiExtensionRegistry extends ExtensionRegistryImpl {

  OgcApiExtensionRegistry() {
    super(null);

    getExtensions().add(new CapabilityQueryables());
    getExtensions().add(new CapabilitySchema());
    getExtensions().add(new CapabilityFeaturesExtensions());
    getExtensions().add(new CapabilityGeoJsonLd());
    getExtensions().add(new CapabilityJsonFg());
    getExtensions().add(new CapabilityFilter());
    getExtensions().add(new CapabilityGeometrySimplification());
    getExtensions().add(new CapabilityMapTiles());
    getExtensions().add(new CapabilityProjections());
    getExtensions().add(new CapabilityResources());
    getExtensions().add(new CapabilitySorting(null, null));
    getExtensions().add(new CapabilityStyles(this)); //TODO: StyleFormatExtensions
    getExtensions().add(new CapabilityTiles(this, null, null, null,
        null)); //TODO: TileFormatWithQuerySupportExtension, TileSetFormatExtension
    getExtensions().add(new CapabilityTransactional());
    getExtensions().add(new CapabilityCollections());
    getExtensions().add(new CapabilityCommon());
    getExtensions().add(new CapabilityCrs());
    getExtensions().add(new CapabilityFeaturesCore());
    getExtensions().add(new CapabilityGeoJson());
    getExtensions().add(new CapabilityGml());
    getExtensions().add(new CapabilityFeaturesHtml());
    getExtensions().add(new CapabilityFoundation());
    getExtensions().add(new CapabilityHtml());
    getExtensions().add(new CapabilityJson());
    getExtensions().add(new CapabilityOpenApi());
    getExtensions().add(new CapabilityXml());
  }
}
