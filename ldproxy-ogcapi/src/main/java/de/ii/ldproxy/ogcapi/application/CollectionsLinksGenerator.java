/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.application;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;

import java.util.List;
import java.util.Optional;

public class CollectionsLinksGenerator extends DefaultLinksGenerator {

    public List<OgcApiLink> generateLinks(URICustomizer uriBuilder, Optional<String> describeFeatureTypeUrl,
                                                     OgcApiMediaType mediaType, List<OgcApiMediaType> alternateMediaTypes)
    {
        final ImmutableList.Builder<OgcApiLink> builder = new ImmutableList.Builder<OgcApiLink>()
                .addAll(super.generateLinks(uriBuilder, mediaType, alternateMediaTypes));

        builder.add(new ImmutableOgcApiLink.Builder()
                .href(uriBuilder
                        .copy()
                        .removeLastPathSegments(1)
                        .ensureNoTrailingSlash()
                        .clearParameters()
                        .toString())
                .rel("home")
                .description("API Landing Page")
                .build());

        if (describeFeatureTypeUrl.isPresent()) {
            builder.add(new ImmutableOgcApiLink.Builder()
                        .href(describeFeatureTypeUrl.get())
                        .rel("describedBy")
                        .type("application/xml")
                        .description("XML schema for the dataset")
                        .build());
        }

        return builder.build();
    }
}
