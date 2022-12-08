/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.DefaultLinksGenerator;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableLink;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class FeaturesLinksGenerator extends DefaultLinksGenerator {

  public static final String LIMIT = "limit";
  public static final String OFFSET = "offset";
  public static final String NEXT = "next";
  public static final String PREV = "prev";
  public static final String FIRST = "first";
  public static final String LANG = "lang";

  public List<Link> generateLinks(
      URICustomizer uriBuilder,
      int offset,
      int limit,
      int defaultLimit,
      ApiMediaType mediaType,
      List<ApiMediaType> alternateMediaTypes,
      I18n i18n,
      Optional<Locale> language) {
    final ImmutableList.Builder<Link> builder =
        new ImmutableList.Builder<Link>()
            .addAll(
                super.generateLinks(uriBuilder, mediaType, alternateMediaTypes, i18n, language));

    uriBuilder.removeParameters(LANG);

    // we have to create a next link here as we do not know numberMatched yet, but it will
    // be removed again in the feature transformer, if we are on the last page
    builder.add(
        new ImmutableLink.Builder()
            .href(getUrlWithPageAndCount(uriBuilder.copy(), offset + limit, limit, defaultLimit))
            .rel(NEXT)
            .type(mediaType.type().toString())
            .title(i18n.get("nextLink", language))
            .build());
    if (offset > 0) {
      builder.add(
          new ImmutableLink.Builder()
              .href(getUrlWithPageAndCount(uriBuilder.copy(), offset - limit, limit, defaultLimit))
              .rel(PREV)
              .type(mediaType.type().toString())
              .title(i18n.get("prevLink", language))
              .build());
      builder.add(
          new ImmutableLink.Builder()
              .href(getUrlWithPageAndCount(uriBuilder.copy(), 0, limit, defaultLimit))
              .rel(FIRST)
              .type(mediaType.type().toString())
              .title(i18n.get("firstLink", language))
              .build());
    }

    return builder.build();
  }

  private String getUrlWithPageAndCount(
      final URICustomizer uriBuilder, final int offset, final int limit, final int defaultLimit) {
    if (offset == 0 && limit == defaultLimit) {
      return uriBuilder.ensureNoTrailingSlash().removeParameters(OFFSET, LIMIT).toString();
    } else if (limit == defaultLimit) {
      return uriBuilder
          .ensureNoTrailingSlash()
          .removeParameters(OFFSET, LIMIT)
          .setParameter(OFFSET, String.valueOf(Integer.max(0, offset)))
          .toString();
    } else if (offset == 0) {
      return uriBuilder
          .ensureNoTrailingSlash()
          .removeParameters(OFFSET, LIMIT)
          .setParameter(LIMIT, String.valueOf(limit))
          .toString();
    }

    return uriBuilder
        .ensureNoTrailingSlash()
        .removeParameters(OFFSET, LIMIT)
        .setParameter(LIMIT, String.valueOf(limit))
        .setParameter(OFFSET, String.valueOf(Integer.max(0, offset)))
        .toString();
  }
}
