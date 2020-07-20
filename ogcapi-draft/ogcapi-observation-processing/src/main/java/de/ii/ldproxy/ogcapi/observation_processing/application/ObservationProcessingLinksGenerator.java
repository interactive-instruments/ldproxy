/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * This class is responsible for generating the links.
 */
public class ObservationProcessingLinksGenerator {

    private static final String DAPA_PATH_ELEMENT = "dapa";

    /**
     * generates the links on the page /{apiId}/collections/{collectionId}
     *
     * @param uriBuilder     the URI, split in host, path and query
     * @param i18n module to get linguistic text
     * @param language the requested language (optional)
     * @return a list with links
     */
    public List<OgcApiLink> generateCollectionLinks(URICustomizer uriBuilder, I18n i18n, Optional<Locale> language) {

        // TODO add links to processing resources
        return ImmutableList.<OgcApiLink>builder()
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                .ensureNoTrailingSlash()
                                .ensureLastPathSegment(DAPA_PATH_ELEMENT)
                                .removeParameters("f")
                                .toString()
                        )
                        .rel("ogc-dapa")
                        .title(i18n.get("dapaEndpointsLink", language))
                        .build())
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                .ensureNoTrailingSlash()
                                .ensureLastPathSegments(DAPA_PATH_ELEMENT, "variables")
                                .removeParameters("f")
                                .toString()
                        )
                        .rel("ogc-variables")
                        .title(i18n.get("variablesLink", language))
                        .build())
                .build();
    }
}
