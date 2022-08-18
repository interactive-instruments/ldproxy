/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.app;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.DefaultLinksGenerator;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableLink;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class StoredQueriesLinkGenerator extends DefaultLinksGenerator {

  /**
   * generates the links on the landing page
   *
   * @param uriBuilder the URI, split in host, path and query
   * @return a list with links
   */
  public List<Link> generateLandingPageLinks(
      URICustomizer uriBuilder, I18n i18n, Optional<Locale> language) {

    ImmutableList.Builder<Link> builder = ImmutableList.builder();

    builder.add(
        new ImmutableLink.Builder()
            .href(
                uriBuilder
                    .copy()
                    .ensureNoTrailingSlash()
                    .ensureLastPathSegment("search")
                    .removeParameters("f")
                    .toString())
            .rel("search")
            .title(i18n.get("storedQueriesLink", language))
            .build());

    return builder.build();
  }

  /**
   * generates the links for a stored query on the page /{serviceId}/search
   *
   * @param uriBuilder the URI, split in host, path and query
   * @param queryId the ids of the queries
   * @return a list with links
   */
  public List<Link> generateStoredQueryLinks(
      URICustomizer uriBuilder, String name, String queryId, I18n i18n, Optional<Locale> language) {

    final ImmutableList.Builder<Link> builder = new ImmutableList.Builder<Link>();

    builder
        .add(
            new ImmutableLink.Builder()
                .href(
                    uriBuilder
                        .copy()
                        .ensureNoTrailingSlash()
                        .ensureLastPathSegment(queryId)
                        .removeParameters("f")
                        .toString())
                .rel("self")
                .title(i18n.get("storedQueryLink", language).replace("{{name}}", name))
                .build())
        .add(
            new ImmutableLink.Builder()
                .href(
                    uriBuilder
                        .copy()
                        .ensureNoTrailingSlash()
                        .ensureLastPathSegments(queryId, "definition")
                        .removeParameters("f")
                        .toString())
                .rel("describedby")
                .title(i18n.get("queryDefinitionLink", language).replace("{{name}}", name))
                .build());

    return builder.build();
  }
}
