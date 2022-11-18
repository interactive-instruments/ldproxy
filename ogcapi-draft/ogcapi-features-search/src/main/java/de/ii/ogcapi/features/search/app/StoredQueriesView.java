/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.app;

import com.google.common.base.Charsets;
import de.ii.ogcapi.features.search.domain.StoredQueries;
import de.ii.ogcapi.features.search.domain.StoredQuery;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.NavigationDTO;
import de.ii.ogcapi.html.domain.OgcApiView;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class StoredQueriesView extends OgcApiView {
  public List<StoredQuery> queries;
  public String typeTitle;
  public String enumTitle;
  public String parametersDescription;
  public String patternTitle;
  public String minTitle;
  public String maxTitle;
  public String defaultTitle;
  public String none;
  public String baseUrl;

  public StoredQueriesView(
      OgcApiDataV2 apiData,
      StoredQueries queries,
      List<NavigationDTO> breadCrumbs,
      String staticUrlPrefix,
      HtmlConfiguration htmlConfig,
      boolean noIndex,
      URICustomizer uriCustomizer,
      I18n i18n,
      Optional<Locale> language) {
    super(
        "storedQueries.mustache",
        Charsets.UTF_8,
        apiData,
        breadCrumbs,
        htmlConfig,
        noIndex,
        staticUrlPrefix,
        queries.getLinks(),
        i18n.get("storedQueriesTitle", language),
        i18n.get("storedQueriesDescription", language));

    this.queries = queries.getQueries();
    this.parametersDescription = i18n.get("parametersDescription", language);
    this.typeTitle = i18n.get("typeTitle", language);
    this.enumTitle = i18n.get("enumTitle", language);
    this.patternTitle = i18n.get("patternTitle", language);
    this.minTitle = i18n.get("minTitle", language);
    this.maxTitle = i18n.get("maxTitle", language);
    this.defaultTitle = i18n.get("defaultTitle", language);
    this.none = i18n.get("none", language);
    this.baseUrl = uriCustomizer.copy().clearParameters().toString();
  }
}
