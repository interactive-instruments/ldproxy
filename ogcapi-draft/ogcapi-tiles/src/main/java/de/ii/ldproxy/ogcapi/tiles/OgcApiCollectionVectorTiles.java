/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;


import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiCollection;
import de.ii.ldproxy.ogcapi.collections.domain.OgcApiCollectionExtension;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Locale;
import java.util.Optional;


/**
 * add tiling information to the collection metadata (supported tiling schemes, links)
 *
 *
 */
@Component
@Provides
@Instantiate
public class OgcApiCollectionVectorTiles implements OgcApiCollectionExtension {

    @Requires
    I18n i18n;

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    @Override
    public ImmutableOgcApiCollection.Builder process(ImmutableOgcApiCollection.Builder collection,
                                                     FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                     OgcApiApiDataV2 apiData,
                                                     URICustomizer uriCustomizer, boolean isNested,
                                                     OgcApiMediaType mediaType,
                                                     List<OgcApiMediaType> alternateMediaTypes,
                                                     Optional<Locale> language) {
        // The hrefs are URI templates and not URIs, so the templates should not be percent encoded!
        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        if (!isNested && isExtensionEnabled(featureTypeConfiguration, TilesConfiguration.class)) {
            collection.addAllLinks(vectorTilesLinkGenerator.generateCollectionLinks(uriCustomizer, i18n, language));
        }

        return collection;
    }

}
