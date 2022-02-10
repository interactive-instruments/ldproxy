/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.maps.app;

import de.ii.ldproxy.ogcapi.common.domain.ImmutableLandingPage;
import de.ii.ldproxy.ogcapi.common.domain.LandingPageExtension;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.maps.domain.MapTileFormatExtension;
import de.ii.ldproxy.ogcapi.maps.domain.MapTilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSet;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * add tiling information to the dataset metadata
 */
@Component
@Provides
@Instantiate
public class MapTilesOnLandingPage implements LandingPageExtension {

    private final I18n i18n;
    private final ExtensionRegistry extensionRegistry;

    public MapTilesOnLandingPage(@Requires I18n i18n,
                                 @Requires ExtensionRegistry extensionRegistry) {
        this.i18n = i18n;
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(MapTilesConfiguration.class)
            .filter(MapTilesConfiguration::getEnabled)
            .filter(MapTilesConfiguration::isMultiCollectionEnabled)
            .isPresent() &&
            apiData.getExtension(TilesConfiguration.class)
                .filter(TilesConfiguration::getEnabled)
                .filter(TilesConfiguration::isMultiCollectionEnabled)
                .isPresent();
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return MapTilesConfiguration.class;
    }

    @Override
    public ImmutableLandingPage.Builder process(ImmutableLandingPage.Builder landingPageBuilder, OgcApiDataV2 apiData,
                                                URICustomizer uriCustomizer, ApiMediaType mediaType,
                                                List<ApiMediaType> alternateMediaTypes,
                                                Optional<Locale> language) {

        if (isEnabledForApi(apiData)) {
            Optional<TileSet.DataType> dataType = extensionRegistry.getExtensionsForType(MapTileFormatExtension.class)
                                                                   .stream()
                                                                   .filter(format -> format.isEnabledForApi(apiData))
                                                                   .map(TileFormatExtension::getDataType)
                                                                   .findAny();
            if (dataType.isEmpty())
                // no tile format is enabled
                return landingPageBuilder;

            final MapTilesLinkGenerator mapTilesLinkGenerator = new MapTilesLinkGenerator();
            List<Link> links = mapTilesLinkGenerator.generateLandingPageLinks(uriCustomizer, dataType.get(), i18n, language);
            landingPageBuilder.addAllLinks(links);
        }
        return landingPageBuilder;
    }
}
