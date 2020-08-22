/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.oas30.app;

import de.ii.ldproxy.ogcapi.common.domain.ImmutableLandingPage;
import de.ii.ldproxy.ogcapi.common.domain.LandingPageExtension;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class LandingPageExtensionOpenApi implements LandingPageExtension {

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return Oas30Configuration.class;
    }

    @Override
    public ImmutableLandingPage.Builder process(ImmutableLandingPage.Builder landingPageBuilder, OgcApiDataV2 apiData,
                                                URICustomizer uriCustomizer, ApiMediaType mediaType,
                                                List<ApiMediaType> alternateMediaTypes, Optional<Locale> language) {

        if (isEnabledForApi(apiData)) {
            // TODO move service-desc/service-doc links from LandingPageLinksGenerator here, once we have multiple API definition options
        }

        return landingPageBuilder;
    }
}
