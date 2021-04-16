/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.ImmutableLink;
import de.ii.ldproxy.ogcapi.domain.Link;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CollectionLinksGenerator extends DefaultLinksGenerator {

    public List<Link> generateLinks(URICustomizer uriBuilder,
                                    ApiMediaType mediaType,
                                    List<ApiMediaType> alternateMediaTypes,
                                    Optional<String> licenseUrl,
                                    Optional<String> licenseName,
                                    I18n i18n,
                                    Optional<Locale> language)
    {
        final ImmutableList.Builder<Link> builder = new ImmutableList.Builder<Link>()
                .addAll(super.generateLinks(uriBuilder, mediaType, alternateMediaTypes, i18n, language));

        if (licenseUrl.isPresent()) {
            builder.add(new ImmutableLink.Builder()
                    .href(licenseUrl.get())
                    .rel("license")
                    .title(licenseName.isPresent() ? licenseName.get() : i18n.get("licenseLink",language))
                    .build());
        }

        return builder.build();
    }
}
