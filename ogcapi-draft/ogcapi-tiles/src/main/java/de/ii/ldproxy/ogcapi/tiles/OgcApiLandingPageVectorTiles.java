/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
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
public class OgcApiLandingPageVectorTiles implements OgcApiLandingPageExtension {

    @Requires
    I18n i18n;

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        Optional<TilesConfiguration> extension = apiData.getExtension(TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::isEnabled)
                .filter(TilesConfiguration::getMultiCollectionEnabled)
                .isPresent();
    }

    @Override
    public ImmutableLandingPage.Builder process(ImmutableLandingPage.Builder landingPageBuilder, OgcApiApiDataV2 apiData,
                                                URICustomizer uriCustomizer, OgcApiMediaType mediaType,
                                                List<OgcApiMediaType> alternateMediaTypes,
                                                Optional<Locale> language) {

        if (checkTilesEnabled(apiData)) {
            final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();
            List<OgcApiLink> ogcApiLinks = vectorTilesLinkGenerator.generateLandingPageLinks(uriCustomizer, i18n, language);
            landingPageBuilder.addAllLinks(ogcApiLinks);
        }
        return landingPageBuilder;
    }

    private boolean checkTilesEnabled(OgcApiApiDataV2 datasetData) {
        return isEnabledForApi(datasetData) &&
               datasetData.getCollections()
                          .values()
                          .stream()
                          .anyMatch(featureTypeConfigurationOgcApi -> isExtensionEnabled(featureTypeConfigurationOgcApi, TilesConfiguration.class));
    }
}
