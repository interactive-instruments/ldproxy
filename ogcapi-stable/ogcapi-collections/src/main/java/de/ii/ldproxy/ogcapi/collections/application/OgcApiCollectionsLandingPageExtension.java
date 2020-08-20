/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.application;

import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.collections.domain.OgcApiCollectionsConfiguration;
import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class OgcApiCollectionsLandingPageExtension implements OgcApiLandingPageExtension {

    @Requires
    I18n i18n;

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, OgcApiCollectionsConfiguration.class);
    }

    @Override
    public ImmutableLandingPage.Builder process(ImmutableLandingPage.Builder landingPageBuilder, OgcApiApiDataV2 apiData,
                                                URICustomizer uriCustomizer,
                                                OgcApiMediaType mediaType,
                                                List<OgcApiMediaType> alternateMediaTypes,
                                                Optional<Locale> language) {

        if (!isEnabledForApi(apiData)) {
            return landingPageBuilder;
        }

        List<String> collectionNames = apiData.getCollections()
                .values()
                .stream()
                .filter(featureType -> featureType.getEnabled())
                .map(featureType -> featureType.getLabel())
                .collect(Collectors.toList());
        String suffix = (collectionNames.size()<=4) ? " ("+String.join(", ", collectionNames)+")" : "";

        landingPageBuilder.addLinks(new ImmutableOgcApiLink.Builder()
                        .href(uriCustomizer.copy()
                                .ensureNoTrailingSlash()
                                .ensureLastPathSegment("collections")
                                .removeParameters("f")
                                .toString())
                        .rel("data")
                        .title(i18n.get("dataLink",language) + suffix)
                        .build());

        return landingPageBuilder;
    }
}
