/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.common.domain.ImmutableLandingPage;
import de.ii.ldproxy.ogcapi.common.domain.LandingPageExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.I18n;
import de.ii.ldproxy.ogcapi.foundation.domain.Link;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.routes.domain.RoutesLinksGenerator;
import de.ii.ldproxy.ogcapi.routes.domain.RoutingConfiguration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * add styles information to the landing page
 *
 */
@Singleton
@AutoBind
public class RoutesOnLandingPage implements LandingPageExtension {

    private final I18n i18n;

    @Inject
    public RoutesOnLandingPage(I18n i18n) {
        this.i18n = i18n;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return RoutingConfiguration.class;
    }

    @Override
    public ImmutableLandingPage.Builder process(ImmutableLandingPage.Builder landingPageBuilder,
                                                OgcApiDataV2 apiData,
                                                URICustomizer uriCustomizer,
                                                ApiMediaType mediaType,
                                                List<ApiMediaType> alternateMediaTypes,
                                                Optional<Locale> language) {

        if (!isEnabledForApi(apiData)) {
            return landingPageBuilder;
        }

        final RoutesLinksGenerator linkGenerator = new RoutesLinksGenerator();
        final List<Link> links = linkGenerator.generateLandingPageLinks(uriCustomizer, i18n, language);
        landingPageBuilder.addAllLinks(links);

        return landingPageBuilder;
    }
}
