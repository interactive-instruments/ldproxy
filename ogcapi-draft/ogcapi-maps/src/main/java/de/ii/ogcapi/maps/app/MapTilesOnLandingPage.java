/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.maps.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.common.domain.ImmutableLandingPage;
import de.ii.ogcapi.common.domain.ImmutableLandingPage.Builder;
import de.ii.ogcapi.common.domain.LandingPageExtension;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.maps.domain.MapTileFormatExtension;
import de.ii.ogcapi.maps.domain.MapTilesConfiguration;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TileSet;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/** add tiling information to the dataset metadata */
@Singleton
@AutoBind
public class MapTilesOnLandingPage implements LandingPageExtension {

  private final I18n i18n;
  private final ExtensionRegistry extensionRegistry;

  @Inject
  public MapTilesOnLandingPage(I18n i18n, ExtensionRegistry extensionRegistry) {
    this.i18n = i18n;
    this.extensionRegistry = extensionRegistry;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
            .getExtension(MapTilesConfiguration.class)
            .filter(MapTilesConfiguration::isEnabled)
            .filter(MapTilesConfiguration::isMultiCollectionEnabled)
            .isPresent()
        && apiData
            .getExtension(TilesConfiguration.class)
            .filter(TilesConfiguration::isEnabled)
            .filter(TilesConfiguration::isMultiCollectionEnabled)
            .isPresent();
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return MapTilesConfiguration.class;
  }

  @Override
  public ImmutableLandingPage.Builder process(
      Builder landingPageBuilder,
      OgcApi api,
      URICustomizer uriCustomizer,
      ApiMediaType mediaType,
      List<ApiMediaType> alternateMediaTypes,
      Optional<Locale> language) {

    if (isEnabledForApi(api.getData())) {
      Optional<TileSet.DataType> dataType =
          extensionRegistry.getExtensionsForType(MapTileFormatExtension.class).stream()
              .filter(
                  format ->
                      format.isApplicable(
                          api.getData(),
                          "/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"))
              .filter(format -> format.isEnabledForApi(api.getData()))
              .map(TileFormatExtension::getDataType)
              .findAny();
      if (dataType.isEmpty())
        // no tile format is enabled
        return landingPageBuilder;

      final MapTilesLinkGenerator mapTilesLinkGenerator = new MapTilesLinkGenerator();
      List<Link> links =
          mapTilesLinkGenerator.generateLandingPageLinks(
              uriCustomizer, dataType.get(), i18n, language);
      landingPageBuilder.addAllLinks(links);
    }
    return landingPageBuilder;
  }
}
