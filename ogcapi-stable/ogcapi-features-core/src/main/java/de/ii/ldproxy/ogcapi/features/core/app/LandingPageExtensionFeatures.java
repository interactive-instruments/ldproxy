/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app;

import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.common.domain.ImmutableLandingPage;
import de.ii.ldproxy.ogcapi.common.domain.LandingPageExtension;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.domain.OgcApiFeaturesCoreConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class LandingPageExtensionFeatures implements LandingPageExtension {

    @Requires
    I18n i18n;

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return OgcApiFeaturesCoreConfiguration.class;
    }

    @Override
    public ImmutableLandingPage.Builder process(ImmutableLandingPage.Builder landingPageBuilder, OgcApiDataV2 apiData,
                                                URICustomizer uriCustomizer,
                                                ApiMediaType mediaType,
                                                List<ApiMediaType> alternateMediaTypes,
                                                Optional<Locale> language) {

        if (!isEnabledForApi(apiData)) {
            return landingPageBuilder;
        }

        Optional<OgcApiFeaturesCoreConfiguration> config = apiData.getExtension(OgcApiFeaturesCoreConfiguration.class);
        if(config.isPresent() && config.get().getAdditionalLinks().containsKey("/")) {
            List<Link> additionalLinks = config.get().getAdditionalLinks().get("/");
            additionalLinks.stream().forEach(link -> landingPageBuilder.addLinks(link));
        }

        return landingPageBuilder;
    }
}
