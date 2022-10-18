/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.app;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.DefaultLinksGenerator;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableLink;
import de.ii.ogcapi.foundation.domain.ImmutableLink.Builder;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

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
   * @param parameterNames
   * @return a list with links
   */
  public List<Link> generateStoredQueryLinks(
      URICustomizer uriBuilder,
      String name,
      String queryId,
      Set<String> parameterNames,
      boolean managerEnabled,
      I18n i18n,
      Optional<Locale> language) {

    final ImmutableList.Builder<Link> builder = new ImmutableList.Builder<Link>();

    Builder selfBuilder =
        new Builder()
            .href(
                uriBuilder
                    .copy()
                    .ensureNoTrailingSlash()
                    .ensureLastPathSegment(queryId)
                    .removeParameters("f")
                    .toString())
            .rel("self")
            .title(i18n.get("storedQueryLink", language).replace("{{name}}", name));

    if (parameterNames.isEmpty()) {
      selfBuilder.href(
          uriBuilder
              .copy()
              .ensureNoTrailingSlash()
              .ensureLastPathSegment(queryId)
              .removeParameters("f")
              .toString());
    } else {
      selfBuilder
          .href(
              uriBuilder
                  .copy()
                  .clearParameters()
                  .ensureNoTrailingSlash()
                  .ensureLastPathSegment(
                      String.format("%s{?%s}", queryId, String.join(",", parameterNames)))
                  .toString())
          .templated(true)
          .varBase(
              uriBuilder
                  .copy()
                  .clearParameters()
                  .ensureLastPathSegments(queryId, "parameters")
                  .ensureTrailingSlash()
                  .toString());
    }

    if (managerEnabled) {
      builder
          .add(selfBuilder.build())
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
    }

    if (!parameterNames.isEmpty()) {
      builder.add(
          new ImmutableLink.Builder()
              .href(
                  uriBuilder
                      .copy()
                      .ensureNoTrailingSlash()
                      .ensureLastPathSegments(queryId, "parameters")
                      .removeParameters("f")
                      .toString())
              .rel("describedby")
              .title(i18n.get("queryParametersLink", language).replace("{{name}}", name))
              .build());

      parameterNames.stream()
          .sorted()
          .forEach(
              parameterName ->
                  builder.add(
                      new ImmutableLink.Builder()
                          .href(
                              uriBuilder
                                  .copy()
                                  .ensureNoTrailingSlash()
                                  .ensureLastPathSegments(queryId, "parameters", parameterName)
                                  .removeParameters("f")
                                  .toString())
                          .rel("describedby")
                          .title(
                              i18n.get("queryParameterLink", language)
                                  .replace("{{name}}", name)
                                  .replace("{{parameter}}", parameterName))
                          .build()));
    }

    return builder.build();
  }

  public List<Link> generateFeaturesLinks(
      URICustomizer uriBuilder,
      int offset,
      int limit,
      ApiMediaType mediaType,
      List<ApiMediaType> alternateMediaTypes,
      I18n i18n,
      Optional<Locale> language) {
    final ImmutableList.Builder<Link> builder =
        new ImmutableList.Builder<Link>()
            .addAll(
                super.generateLinks(uriBuilder, mediaType, alternateMediaTypes, i18n, language));

    uriBuilder.removeParameters("lang");

    // we have to create a next link here as we do not know numberMatched yet, but it will
    // be removed again in the feature transformer, if we are on the last page
    builder.add(
        new ImmutableLink.Builder()
            .href(getUrlWithOffset(uriBuilder.copy(), offset + limit))
            .rel("next")
            .type(mediaType.type().toString())
            .title(i18n.get("nextLink", language))
            .build());
    if (offset > 0) {
      builder.add(
          new ImmutableLink.Builder()
              .href(getUrlWithOffset(uriBuilder.copy(), offset - limit))
              .rel("prev")
              .type(mediaType.type().toString())
              .title(i18n.get("prevLink", language))
              .build());
      builder.add(
          new ImmutableLink.Builder()
              .href(getUrlWithOffset(uriBuilder.copy(), 0))
              .rel("first")
              .type(mediaType.type().toString())
              .title(i18n.get("firstLink", language))
              .build());
    }

    return builder.build();
  }

  private String getUrlWithOffset(final URICustomizer uriBuilder, final int offset) {
    if (offset == 0) {
      return uriBuilder.ensureNoTrailingSlash().removeParameters("offset").toString();
    }

    return uriBuilder
        .ensureNoTrailingSlash()
        .removeParameters("offset")
        .setParameter("offset", String.valueOf(Integer.max(0, offset)))
        .toString();
  }
}
