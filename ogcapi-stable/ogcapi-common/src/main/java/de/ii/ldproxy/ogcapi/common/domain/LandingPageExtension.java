/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ContentExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.URICustomizer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@AutoMultiBind
public interface LandingPageExtension extends ContentExtension {

    ImmutableLandingPage.Builder process(ImmutableLandingPage.Builder landingPageBuilder,
                                         OgcApiDataV2 apiData,
                                         URICustomizer uriCustomizer,
                                         ApiMediaType mediaType,
                                         List<ApiMediaType> alternateMediaTypes,
                                         Optional<Locale> language);

    default String getResourceName() { return "Landing Page"; };
}
