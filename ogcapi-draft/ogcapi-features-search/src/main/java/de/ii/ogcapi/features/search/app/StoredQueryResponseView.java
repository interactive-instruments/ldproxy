/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.app;

import de.ii.ogcapi.features.html.domain.FeatureCollectionBaseView;
import de.ii.ogcapi.features.html.domain.FeaturesHtmlConfiguration.POSITION;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.MapClient.Type;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class StoredQueryResponseView extends FeatureCollectionBaseView {
  public String typeTitle;
  public String enumTitle;
  public String parametersDescription;
  public String patternTitle;
  public String minTitle;
  public String maxTitle;
  public String defaultTitle;
  public String none;

  public StoredQueryResponseView(
      Optional<BoundingBox> spatialExtent,
      String template,
      URI uri,
      String id,
      String title,
      String description,
      String attribution,
      String urlPrefix,
      HtmlConfiguration htmlConfig,
      String persistentUri,
      boolean noIndex,
      I18n i18n,
      Optional<Locale> language,
      POSITION mapPosition,
      Type mapClientType,
      String styleUrl,
      boolean removeZoomLevelConstraints,
      boolean hideMap,
      List<String> geometryProperties) {
    super(
        spatialExtent,
        template,
        uri,
        id,
        title,
        description,
        attribution,
        urlPrefix,
        htmlConfig,
        persistentUri,
        noIndex,
        mapPosition,
        mapClientType,
        styleUrl,
        removeZoomLevelConstraints,
        hideMap,
        geometryProperties);

    this.fromStoredQuery = true;
    this.parametersDescription = i18n.get("parametersDescription", language);
    this.typeTitle = i18n.get("typeTitle", language);
    this.enumTitle = i18n.get("enumTitle", language);
    this.patternTitle = i18n.get("patternTitle", language);
    this.minTitle = i18n.get("minTitle", language);
    this.maxTitle = i18n.get("maxTitle", language);
    this.defaultTitle = i18n.get("defaultTitle", language);
    this.none = i18n.get("none", language);
  }
}
