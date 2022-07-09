/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.ogcapi.collections.domain.ImmutableCollectionsConfiguration;
import de.ii.ogcapi.collections.queryables.domain.ImmutableQueryablesConfiguration;
import de.ii.ogcapi.collections.schema.domain.ImmutableSchemaConfiguration;
import de.ii.ogcapi.common.domain.ImmutableCommonConfiguration;
import de.ii.ogcapi.crs.domain.ImmutableCrsConfiguration;
import de.ii.ogcapi.features.core.domain.ImmutableFeaturesCoreConfiguration;
import de.ii.ogcapi.features.custom.extensions.domain.ImmutableFeaturesExtensionsConfiguration;
import de.ii.ogcapi.features.flatgeobuf.domain.ImmutableFlatgeobufConfiguration;
import de.ii.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration;
import de.ii.ogcapi.features.geojson.ld.domain.ImmutableGeoJsonLdConfiguration;
import de.ii.ogcapi.features.gml.app.ImmutableGmlConfiguration;
import de.ii.ogcapi.features.html.domain.ImmutableFeaturesHtmlConfiguration;
import de.ii.ogcapi.features.json.fg.domain.ImmutableJsonFgConfiguration;
import de.ii.ogcapi.filter.domain.ImmutableFilterConfiguration;
import de.ii.ogcapi.foundation.domain.ImmutableFoundationConfiguration;
import de.ii.ogcapi.geometry.simplification.app.ImmutableGeometrySimplificationConfiguration;
import de.ii.ogcapi.html.domain.ImmutableHtmlConfiguration;
import de.ii.ogcapi.json.domain.ImmutableJsonConfiguration;
import de.ii.ogcapi.maps.domain.ImmutableMapTilesConfiguration;
import de.ii.ogcapi.oas30.domain.ImmutableOas30Configuration;
import de.ii.ogcapi.projections.app.ImmutableProjectionsConfiguration;
import de.ii.ogcapi.resources.domain.ImmutableResourcesConfiguration;
import de.ii.ogcapi.sorting.domain.ImmutableSortingConfiguration;
import de.ii.ogcapi.styles.domain.ImmutableStylesConfiguration;
import de.ii.ogcapi.tiles.domain.ImmutableTilesConfiguration;
import de.ii.ogcapi.crud.app.ImmutableCrudConfiguration;
import de.ii.ogcapi.xml.domain.ImmutableXmlConfiguration;

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

  default ImmutableFlatgeobufConfiguration.Builder flatgeobuf() {
    return new ImmutableFlatgeobufConfiguration.Builder();
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

  default ImmutableCrudConfiguration.Builder transactional() {
    return new ImmutableCrudConfiguration.Builder();
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
