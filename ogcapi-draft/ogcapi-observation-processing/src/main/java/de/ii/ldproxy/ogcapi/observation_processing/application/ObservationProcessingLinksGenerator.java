/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * This class is responsible for generating the links.
 */
public class ObservationProcessingLinksGenerator extends DefaultLinksGenerator {

    private static final String DAPA_PATH_ELEMENT = "processes";

    /**
     * generates the links on the page /{apiId}/collections/{collectionId}
     *
     * @param featureType the feature collection
     * @param uriBuilder     the URI, split in host, path and query
     * @param i18n module to get linguistic text
     * @param language the requested language (optional)
     * @return a list with links
     */
    public List<Link> generateCollectionLinks(FeatureTypeConfigurationOgcApi featureType, URICustomizer uriBuilder, I18n i18n, Optional<Locale> language) {

        return ImmutableList.<Link>builder()
                .add(new ImmutableLink.Builder()
                        .href(uriBuilder.copy()
                                .ensureNoTrailingSlash()
                                .ensureLastPathSegment(DAPA_PATH_ELEMENT)
                                .removeParameters("f")
                                .toString()
                        )
                        .rel("ogc-dapa-processes")
                        .title(i18n.get("dapaEndpointsLink", language).replace("{{collection}}", featureType.getLabel()))
                        .build())
                .add(new ImmutableLink.Builder()
                        .href(uriBuilder.copy()
                                .ensureNoTrailingSlash()
                                .ensureLastPathSegments("variables")
                                .removeParameters("f")
                                .toString()
                        )
                        .rel("ogc-dapa-variables")
                        .title(i18n.get("variablesLink", language))
                        .build())
                .build();
    }

    /**
     * generates the links on the page /{apiId}/collections/{collectionId}
     *
     * @param uriBuilder     the URI, split in host, path and query
     * @param i18n module to get linguistic text
     * @param language the requested language (optional)
     * @return a list with links
     */
    public List<Link> generateDapaLinks(URICustomizer uriBuilder,
                                        ApiMediaType mediaType, List<ApiMediaType> alternateMediaTypes,
                                        I18n i18n, Optional<Locale> language) {

        return ImmutableList.<Link>builder()
                .addAll(super.generateLinks(uriBuilder, mediaType, alternateMediaTypes, i18n, language))
                .add(new ImmutableLink.Builder()
                        .href(uriBuilder.copy()
                                        .removeLastPathSegments(1)
                                        .ensureNoTrailingSlash()
                                        .ensureLastPathSegment("variables")
                                        .removeParameters("f")
                                        .toString()
                        )
                        .rel("ogc-dapa-variables")
                        .title(i18n.get("variablesLink", language))
                        .build())
                .build();
    }

    /**
     * generates the links on the page /{apiId}
     *
     * @param featureType the feature collection
     * @param uriBuilder     the URI, split in host, path and query
     * @param i18n module to get linguistic text
     * @param language the requested language (optional)
     * @return a list with links
     */
    public List<Link> generateLandingPageLinks(FeatureTypeConfigurationOgcApi featureType, URICustomizer uriBuilder, I18n i18n, Optional<Locale> language) {

        return ImmutableList.<Link>builder()
                .add(new ImmutableLink.Builder()
                        .href(uriBuilder.copy()
                                .ensureNoTrailingSlash()
                                .ensureLastPathSegments("collections", featureType.getId(), DAPA_PATH_ELEMENT)
                                .removeParameters("f")
                                .toString()
                        )
                        .rel("ogc-dapa-processes")
                        .title(i18n.get("dapaEndpointsLink", language).replace("{{collection}}", featureType.getLabel()))
                        .build())
                .build();
    }
}
