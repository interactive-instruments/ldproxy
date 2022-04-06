/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.collections.app.CapabilityCollections;
import de.ii.ogcapi.collections.queryables.app.CapabilityQueryables;
import de.ii.ogcapi.collections.schema.app.CapabilitySchema;
import de.ii.ogcapi.common.domain.CapabilityCommon;
import de.ii.ogcapi.crs.app.CapabilityCrs;
import de.ii.ogcapi.features.core.app.CapabilityFeaturesCore;
import de.ii.ogcapi.features.custom.extensions.app.CapabilityFeaturesExtensions;
import de.ii.ogcapi.features.flatgeobuf.app.CapabilityFlatgeobuf;
import de.ii.ogcapi.features.geojson.app.CapabilityGeoJson;
import de.ii.ogcapi.features.geojson.ld.app.CapabilityGeoJsonLd;
import de.ii.ogcapi.features.gml.app.CapabilityGml;
import de.ii.ogcapi.features.html.app.CapabilityFeaturesHtml;
import de.ii.ogcapi.features.json.fg.app.CapabilityJsonFg;
import de.ii.ogcapi.filter.app.CapabilityFilter;
import de.ii.ogcapi.foundation.domain.ApiExtension;
import de.ii.ogcapi.foundation.domain.CapabilityFoundation;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.geometry.simplification.app.CapabilityGeometrySimplification;
import de.ii.ogcapi.html.app.CapabilityHtml;
import de.ii.ogcapi.json.app.CapabilityJson;
import de.ii.ogcapi.maps.app.CapabilityMapTiles;
import de.ii.ogcapi.oas30.app.CapabilityOpenApi;
import de.ii.ogcapi.projections.app.CapabilityProjections;
import de.ii.ogcapi.resources.app.CapabilityResources;
import de.ii.ogcapi.sorting.app.CapabilitySorting;
import de.ii.ogcapi.styles.app.CapabilityStyles;
import de.ii.ogcapi.tiles.app.CapabilityTiles;
import de.ii.ogcapi.transactional.app.CapabilityTransactional;
import de.ii.ogcapi.xml.app.CapabilityXml;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class OgcApiExtensionRegistry implements ExtensionRegistry {

  private final Set<ApiExtension> apiExtensions;

  OgcApiExtensionRegistry() {
    this.apiExtensions = ImmutableSet.<ApiExtension>builder()
        .add(new CapabilityQueryables())
        .add(new CapabilitySchema())
        .add(new CapabilityFeaturesExtensions())
        .add(new CapabilityFlatgeobuf())
        .add(new CapabilityGeoJsonLd())
        .add(new CapabilityJsonFg())
        .add(new CapabilityFilter())
        .add(new CapabilityGeometrySimplification())
        .add(new CapabilityMapTiles())
        .add(new CapabilityProjections())
        .add(new CapabilityResources())
        .add(new CapabilitySorting(null, null))
        .add(new CapabilityStyles(this)) // TODO: StyleFormatExtensions
        .add(
            new CapabilityTiles(
                this, null, null, null,
                null)) // TODO: TileFormatWithQuerySupportExtension,
        // TileSetFormatExtension
        .add(new CapabilityTransactional())
        .add(new CapabilityCollections())
        .add(new CapabilityCommon())
        .add(new CapabilityCrs(null, null))
        .add(new CapabilityFeaturesCore(null, null))
        .add(new CapabilityGeoJson())
        .add(new CapabilityGml())
        .add(new CapabilityFeaturesHtml())
        .add(new CapabilityFoundation())
        .add(new CapabilityHtml())
        .add(new CapabilityJson())
        .add(new CapabilityOpenApi())
        .add(new CapabilityXml())
        .build();
  }

  @Override
  public List<ApiExtension> getExtensions() {
    return ImmutableList.copyOf(apiExtensions);
  }

  @Override
  public <T extends ApiExtension> List<T> getExtensionsForType(Class<T> extensionType) {
    return apiExtensions.stream()
        .filter(extension -> extension!=null && extensionType.isAssignableFrom(extension.getClass()))
        .map(extensionType::cast)
        .collect(Collectors.toList());
  }
}
