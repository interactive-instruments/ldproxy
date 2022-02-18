/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;


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
import de.ii.ldproxy.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSet;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
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
public class TilesOnCollection implements CollectionExtension {

    private final I18n i18n;
    private final ExtensionRegistry extensionRegistry;

    @Inject
    public TilesOnCollection(I18n i18n,
                             ExtensionRegistry extensionRegistry) {
        this.i18n = i18n;
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
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
        final TilesLinkGenerator tilesLinkGenerator = new TilesLinkGenerator();

        if (!isNested && isExtensionEnabled(featureTypeConfiguration, TilesConfiguration.class) &&
            isExtensionEnabled(featureTypeConfiguration, TilesConfiguration.class, TilesConfiguration::isSingleCollectionEnabled)) {
            Optional<TileSet.DataType> dataType = extensionRegistry.getExtensionsForType(TileFormatExtension.class)
                                                                   .stream()
                                                                   .filter(format -> format.isEnabledForApi(apiData, featureTypeConfiguration.getId()))
                                                                   .map(format -> format.getDataType())
                                                                   .findAny();
            if (dataType.isEmpty())
                // no tile format is enabled
                return collection;

            collection.addAllLinks(tilesLinkGenerator.generateCollectionLinks(uriCustomizer, dataType.get(), i18n, language));
        }

        return collection;
    }

}
