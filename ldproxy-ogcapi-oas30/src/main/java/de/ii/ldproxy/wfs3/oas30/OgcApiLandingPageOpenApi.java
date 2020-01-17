/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.oas30;

import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.List;
import java.util.Locale;
import java.util.Optional;


@Component
@Provides
@Instantiate
public class OgcApiLandingPageOpenApi implements OgcApiLandingPageExtension {

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, Oas30Configuration.class);
    }

    @Override
    public ImmutableLandingPage.Builder process(ImmutableLandingPage.Builder landingPageBuilder, OgcApiApiDataV2 apiData,
                                                URICustomizer uriCustomizer, OgcApiMediaType mediaType,
                                                List<OgcApiMediaType> alternateMediaTypes, Optional<Locale> language) {

        /* TODO
        if (isEnabledForApi(apiData)) {
            landingPageBuilder.addSections(ImmutableMap.of("title", "API Definition", "links",
                                           ImmutableList.of(ImmutableMap.of("title", "OpenAPI 3.0", "url",
                                                            uriCustomizer.ensureLastPathSegment("api")
                                                                         .toString()))));
        }
        */

        return landingPageBuilder;
    }
}
