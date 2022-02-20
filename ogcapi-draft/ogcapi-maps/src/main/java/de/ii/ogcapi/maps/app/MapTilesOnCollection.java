/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.maps.app;


import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionExtension;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiCollection;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.I18n;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.URICustomizer;
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


/**
 * add tiling information to the collection metadata (supported tiling schemes, links)
 *
 *
 */
@Singleton
@AutoBind
public class MapTilesOnCollection implements CollectionExtension {

    private final I18n i18n;
    private final ExtensionRegistry extensionRegistry;

    @Inject
    public MapTilesOnCollection(I18n i18n,
                                ExtensionRegistry extensionRegistry) {
        this.i18n = i18n;
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return MapTilesConfiguration.class;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return apiData.getExtension(MapTilesConfiguration.class)
            .filter(MapTilesConfiguration::getEnabled)
            .filter(MapTilesConfiguration::isSingleCollectionEnabled)
            .isPresent() &&
            apiData.getExtension(TilesConfiguration.class)
                .filter(TilesConfiguration::getEnabled)
                .filter(TilesConfiguration::isSingleCollectionEnabled)
                .isPresent();
    }

    @Override
    public ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                                     FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                     OgcApiDataV2 apiData,
                                                     URICustomizer uriCustomizer, boolean isNested,
                                                     ApiMediaType mediaType,
                                                     List<ApiMediaType> alternateMediaTypes,
                                                     Optional<Locale> language) {
        // The hrefs are URI templates and not URIs, so the templates should not be percent encoded!
        final MapTilesLinkGenerator mapTilesLinkGenerator = new MapTilesLinkGenerator();

        if (!isNested && isEnabledForApi(apiData, featureTypeConfiguration.getId())) {
            Optional<TileSet.DataType> dataType = extensionRegistry.getExtensionsForType(MapTileFormatExtension.class)
                                                                   .stream()
                                                                   .filter(format -> format.isEnabledForApi(apiData, featureTypeConfiguration.getId()))
                                                                   .map(TileFormatExtension::getDataType)
                                                                   .findAny();
            if (dataType.isEmpty())
                // no tile format is enabled
                return collection;

            collection.addAllLinks(mapTilesLinkGenerator.generateCollectionLinks(uriCustomizer, dataType.get(), i18n, language));
        }

        return collection;
    }
}
