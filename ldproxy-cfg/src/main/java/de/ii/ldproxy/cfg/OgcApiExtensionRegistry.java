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
import de.ii.ogcapi.collections.app.CollectionsBuildingBlock;
import de.ii.ogcapi.collections.queryables.app.QueryablesBuildingBlock;
import de.ii.ogcapi.collections.schema.app.SchemaBuildingBlock;
import de.ii.ogcapi.common.domain.CommonBuildingBlock;
import de.ii.ogcapi.crs.app.CrsBuildingBlock;
import de.ii.ogcapi.features.core.app.FeaturesCoreBuildingBlock;
import de.ii.ogcapi.features.custom.extensions.app.FeaturesExtensionsBuildingBlock;
import de.ii.ogcapi.features.flatgeobuf.app.CapabilityFlatgeobuf;
import de.ii.ogcapi.features.geojson.app.GeoJsonBuildingBlock;
import de.ii.ogcapi.features.geojson.ld.app.GeoJsonLdBuildingBlock;
import de.ii.ogcapi.features.gml.app.GmlBuildingBlock;
import de.ii.ogcapi.features.html.app.FeaturesHtmlBuildingBlock;
import de.ii.ogcapi.features.json.fg.app.JsonFgBuildingBlock;
import de.ii.ogcapi.filter.app.FilterBuildingBlock;
import de.ii.ogcapi.foundation.domain.ApiExtension;
import de.ii.ogcapi.foundation.domain.FoundationBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.geometry.simplification.app.GeometrySimplificationBuildingBlock;
import de.ii.ogcapi.html.app.HtmlBuildingBlock;
import de.ii.ogcapi.json.app.JsonBuildingBlock;
import de.ii.ogcapi.maps.app.MapTilesBuildingBlock;
import de.ii.ogcapi.oas30.app.OpenApiBuildingBlock;
import de.ii.ogcapi.projections.app.ProjectionsBuildingBlock;
import de.ii.ogcapi.resources.app.ResourcesBuildingBlock;
import de.ii.ogcapi.sorting.app.SortingBuildingBlock;
import de.ii.ogcapi.styles.app.StylesBuildingBlock;
import de.ii.ogcapi.tiles.app.TilesBuildingBlock;
import de.ii.ogcapi.transactional.app.TransactionalBuildingBlock;
import de.ii.ogcapi.xml.app.XmlBuildingBlock;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class OgcApiExtensionRegistry implements ExtensionRegistry {

  private final Set<ApiExtension> apiExtensions;

  OgcApiExtensionRegistry() {
    this.apiExtensions = ImmutableSet.<ApiExtension>builder()
        .add(new QueryablesBuildingBlock())
        .add(new SchemaBuildingBlock())
        .add(new FeaturesExtensionsBuildingBlock())
        .add(new CapabilityFlatgeobuf())
        .add(new GeoJsonLdBuildingBlock())
        .add(new JsonFgBuildingBlock())
        .add(new FilterBuildingBlock())
        .add(new GeometrySimplificationBuildingBlock())
        .add(new MapTilesBuildingBlock())
        .add(new ProjectionsBuildingBlock())
        .add(new ResourcesBuildingBlock())
        .add(new SortingBuildingBlock(null, null))
        .add(new StylesBuildingBlock(this)) // TODO: StyleFormatExtensions
        .add(
            new TilesBuildingBlock(
                this, null, null, null,
                null)) // TODO: TileFormatWithQuerySupportExtension,
        // TileSetFormatExtension
        .add(new TransactionalBuildingBlock())
        .add(new CollectionsBuildingBlock())
        .add(new CommonBuildingBlock())
        .add(new CrsBuildingBlock(null, null))
        .add(new FeaturesCoreBuildingBlock(null, null))
        .add(new GeoJsonBuildingBlock())
        .add(new GmlBuildingBlock())
        .add(new FeaturesHtmlBuildingBlock())
        .add(new FoundationBuildingBlock())
        .add(new HtmlBuildingBlock())
        .add(new JsonBuildingBlock())
        .add(new OpenApiBuildingBlock())
        .add(new XmlBuildingBlock())
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
