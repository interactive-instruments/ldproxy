/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.common.domain.ImmutableLandingPage;
import de.ii.ldproxy.ogcapi.common.domain.LandingPageExtension;
import de.ii.ldproxy.ogcapi.domain.*;
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
public class VectorTilesOnLandingPage implements LandingPageExtension {

    private final I18n i18n;

    public VectorTilesOnLandingPage(@Requires I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        Optional<TilesConfiguration> extension = apiData.getExtension(TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::isEnabled)
                .filter(TilesConfiguration::getMultiCollectionEnabledDerived)
                .isPresent();
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    @Override
    public ImmutableLandingPage.Builder process(ImmutableLandingPage.Builder landingPageBuilder, OgcApiDataV2 apiData,
                                                URICustomizer uriCustomizer, ApiMediaType mediaType,
                                                List<ApiMediaType> alternateMediaTypes,
                                                Optional<Locale> language) {

        if (isEnabledForApi(apiData)) {
            final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();
            List<Link> links = vectorTilesLinkGenerator.generateLandingPageLinks(uriCustomizer, i18n, language);
            landingPageBuilder.addAllLinks(links);
        }
        return landingPageBuilder;
    }

    private boolean checkTilesEnabled(OgcApiDataV2 apiData) {
        return isEnabledForApi(apiData) &&
                apiData.getCollections()
                          .values()
                          .stream()
                          .anyMatch(featureTypeConfigurationOgcApi -> isExtensionEnabled(featureTypeConfigurationOgcApi, TilesConfiguration.class));
    }
}
