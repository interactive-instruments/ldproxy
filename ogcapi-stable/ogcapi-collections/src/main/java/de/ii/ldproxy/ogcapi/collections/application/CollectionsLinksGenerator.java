/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.application;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CollectionsLinksGenerator extends DefaultLinksGenerator {

    public List<OgcApiLink> generateLinks(URICustomizer uriBuilder,
                                          Optional<String> describeFeatureTypeUrl,
                                          OgcApiMediaType mediaType,
                                          List<OgcApiMediaType> alternateMediaTypes,
                                          Optional<String> licenseUrl,
                                          Optional<String> licenseName,
                                          boolean homeLink,
                                          I18n i18n,
                                          Optional<Locale> language)
    {
        final ImmutableList.Builder<OgcApiLink> builder = new ImmutableList.Builder<OgcApiLink>()
                .addAll(super.generateLinks(uriBuilder, mediaType, alternateMediaTypes, i18n, language));

        if (homeLink)
            builder.add(new ImmutableOgcApiLink.Builder()
                    .href(uriBuilder
                            .copy()
                            .removeLastPathSegments(1)
                            .ensureNoTrailingSlash()
                            .clearParameters()
                            .toString())
                    .rel("home")
                    .title(i18n.get("homeLink",language))
                    .build());

        if (licenseUrl.isPresent()) {
            builder.add(new ImmutableOgcApiLink.Builder()
                    .href(licenseUrl.get())
                    .rel("license")
                    .title(licenseName.isPresent() ? licenseName.get() : i18n.get("licenseLink",language))
                    .build());
        }

        if (describeFeatureTypeUrl.isPresent()) {
            builder.add(new ImmutableOgcApiLink.Builder()
                        .href(describeFeatureTypeUrl.get())
                        .rel("describedby")
                        .type("application/xml")
                        .title(i18n.get("describedByXsdLink",language))
                        .build());
        }

        return builder.build();
    }
}
