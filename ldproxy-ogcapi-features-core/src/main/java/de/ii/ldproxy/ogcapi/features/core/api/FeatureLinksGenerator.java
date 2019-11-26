/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.api;

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

public class FeatureLinksGenerator extends DefaultLinksGenerator {

    public List<OgcApiLink> generateLinks(URICustomizer uriBuilder,
                                          OgcApiMediaType mediaType,
                                          List<OgcApiMediaType> alternateMediaTypes,
                                          OgcApiMediaType collectionMediaType,
                                          String canonicalUri,
                                          boolean homeLink,
                                          I18n i18n,
                                          Optional<Locale> language)
    {
        final ImmutableList.Builder<OgcApiLink> builder = new ImmutableList.Builder<OgcApiLink>()
                .addAll(super.generateLinks(uriBuilder, mediaType, alternateMediaTypes, i18n, language));

        if (canonicalUri!=null)
            builder.add(new ImmutableOgcApiLink.Builder()
                    .href(canonicalUri)
                    .rel("canonical")
                    .title(i18n.get("persistentLink",language))
                    .build());

        builder.add(new ImmutableOgcApiLink.Builder()
                .href(uriBuilder
                        .copy()
                        .clearParameters()
                        .ensureParameter("f",collectionMediaType.parameter())
                        .removeLastPathSegments(2)
                        .toString())
                .rel("collection")
                .type(collectionMediaType.type().toString())
                .title(i18n.get("collectionLink",language))
                .build());

        if (homeLink)
            builder.add(new ImmutableOgcApiLink.Builder()
                    .href(uriBuilder
                            .copy()
                            .removeLastPathSegments(4)
                            .ensureNoTrailingSlash()
                            .clearParameters()
                            .toString())
                    .rel("home")
                    .title(i18n.get("homeLink",language))
                    .build());

        return builder.build();
    }
}
