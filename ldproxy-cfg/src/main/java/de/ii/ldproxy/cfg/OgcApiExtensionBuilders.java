/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.ldproxy.ogcapi.collections.domain.ImmutableCollectionsConfiguration;
import de.ii.ldproxy.ogcapi.collections.queryables.domain.ImmutableQueryablesConfiguration;
import de.ii.ldproxy.ogcapi.collections.schema.domain.ImmutableSchemaConfiguration;
import de.ii.ldproxy.ogcapi.common.domain.ImmutableCommonConfiguration;
import de.ii.ldproxy.ogcapi.crs.domain.ImmutableCrsConfiguration;
import de.ii.ldproxy.ogcapi.domain.ImmutableFoundationConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutableFeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.extensions.domain.ImmutableFeaturesExtensionsConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration;
import de.ii.ldproxy.ogcapi.features.geojsonld.domain.ImmutableGeoJsonLdConfiguration;
import de.ii.ldproxy.ogcapi.features.gml.app.ImmutableGmlConfiguration;
import de.ii.ldproxy.ogcapi.features.html.domain.ImmutableFeaturesHtmlConfiguration;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.ImmutableJsonFgConfiguration;
import de.ii.ldproxy.ogcapi.features.transactional.ImmutableTransactionalConfiguration;
import de.ii.ldproxy.ogcapi.filter.domain.ImmutableFilterConfiguration;
import de.ii.ldproxy.ogcapi.geometry_simplification.ImmutableGeometrySimplificationConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.ImmutableHtmlConfiguration;
import de.ii.ldproxy.ogcapi.json.domain.ImmutableJsonConfiguration;
import de.ii.ldproxy.ogcapi.maps.domain.ImmutableMapTilesConfiguration;
import de.ii.ldproxy.ogcapi.oas30.domain.ImmutableOas30Configuration;
import de.ii.ldproxy.ogcapi.projections.ImmutableProjectionsConfiguration;
import de.ii.ldproxy.ogcapi.sorting.ImmutableSortingConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableStylesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableTilesConfiguration;
import de.ii.ldproxy.ogcapi.xml.domain.ImmutableXmlConfiguration;
import de.ii.ldproxy.resources.domain.ImmutableResourcesConfiguration;

public interface OgcApiExtensionBuilders {

  default ImmutableQueryablesConfiguration.Builder queryables() {
    return new ImmutableQueryablesConfiguration.Builder();
  }

  default ImmutableSchemaConfiguration.Builder schema() {
    return new ImmutableSchemaConfiguration.Builder();
  }

  default ImmutableFeaturesExtensionsConfiguration.Builder featuresExtensions() {
    return new ImmutableFeaturesExtensionsConfiguration.Builder();
  }

  default ImmutableGeoJsonLdConfiguration.Builder geoJsonLd() {
    return new ImmutableGeoJsonLdConfiguration.Builder();
  }

  default ImmutableJsonFgConfiguration.Builder jsonFg() {
    return new ImmutableJsonFgConfiguration.Builder();
  }

  default ImmutableFilterConfiguration.Builder filter() {
    return new ImmutableFilterConfiguration.Builder();
  }

  default ImmutableGeometrySimplificationConfiguration.Builder geometrySimplification() {
    return new ImmutableGeometrySimplificationConfiguration.Builder();
  }

  default ImmutableMapTilesConfiguration.Builder mapTiles() {
    return new ImmutableMapTilesConfiguration.Builder();
  }

  default ImmutableProjectionsConfiguration.Builder projections() {
    return new ImmutableProjectionsConfiguration.Builder();
  }

  default ImmutableResourcesConfiguration.Builder resources() {
    return new ImmutableResourcesConfiguration.Builder();
  }

  default ImmutableSortingConfiguration.Builder sorting() {
    return new ImmutableSortingConfiguration.Builder();
  }

  default ImmutableStylesConfiguration.Builder styles() {
    return new ImmutableStylesConfiguration.Builder();
  }

  default ImmutableTilesConfiguration.Builder tiles() {
    return new ImmutableTilesConfiguration.Builder();
  }

  default ImmutableTransactionalConfiguration.Builder transactional() {
    return new ImmutableTransactionalConfiguration.Builder();
  }

  default ImmutableCollectionsConfiguration.Builder collections() {
    return new ImmutableCollectionsConfiguration.Builder();
  }

  default ImmutableCommonConfiguration.Builder common() {
    return new ImmutableCommonConfiguration.Builder();
  }

  default ImmutableCrsConfiguration.Builder crs() {
    return new ImmutableCrsConfiguration.Builder();
  }

  default ImmutableFeaturesCoreConfiguration.Builder featuresCore() {
    return new ImmutableFeaturesCoreConfiguration.Builder();
  }

  default ImmutableGeoJsonConfiguration.Builder geoJson() {
    return new ImmutableGeoJsonConfiguration.Builder();
  }

  default ImmutableGmlConfiguration.Builder gml() {
    return new ImmutableGmlConfiguration.Builder();
  }

  default ImmutableFeaturesHtmlConfiguration.Builder featuresHtml() {
    return new ImmutableFeaturesHtmlConfiguration.Builder();
  }

  default ImmutableFoundationConfiguration.Builder foundation() {
    return new ImmutableFoundationConfiguration.Builder();
  }

  default ImmutableHtmlConfiguration.Builder html() {
    return new ImmutableHtmlConfiguration.Builder();
  }

  default ImmutableJsonConfiguration.Builder json() {
    return new ImmutableJsonConfiguration.Builder();
  }

  default ImmutableOas30Configuration.Builder oas30() {
    return new ImmutableOas30Configuration.Builder();
  }

  default ImmutableXmlConfiguration.Builder xml() {
    return new ImmutableXmlConfiguration.Builder();
  }
}
