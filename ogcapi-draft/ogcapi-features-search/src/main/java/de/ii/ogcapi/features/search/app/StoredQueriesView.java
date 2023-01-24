/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.app;

import de.ii.ogcapi.features.search.domain.StoredQuery;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.html.domain.OgcApiView;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
public abstract class StoredQueriesView extends OgcApiView {

  StoredQueriesView() {
    super("storedQueries.mustache");
  }

  public abstract List<StoredQuery> queries();

  public abstract I18n i18n();

  public abstract Optional<Locale> language();

  public abstract String baseUrl();

  @Value.Derived
  public boolean isForm() {
    return true;
  }

  @Value.Derived
  @Override
  public String title() {
    return i18n().get("storedQueriesTitle", language());
  }

  @Value.Derived
  @Override
  public String description() {
    return i18n().get("storedQueriesDescription", language());
  }

  @Value.Derived
  public String typeTitle() {
    return i18n().get("typeTitle", language());
  }

  @Value.Derived
  public String enumTitle() {
    return i18n().get("enumTitle", language());
  }

  @Value.Derived
  public String parametersDescription() {
    return i18n().get("parametersDescription", language());
  }

  @Value.Derived
  public String patternTitle() {
    return i18n().get("patternTitle", language());
  }

  @Value.Derived
  public String minTitle() {
    return i18n().get("minTitle", language());
  }

  @Value.Derived
  public String maxTitle() {
    return i18n().get("maxTitle", language());
  }

  @Value.Derived
  public String defaultTitle() {
    return i18n().get("defaultTitle", language());
  }

  @Value.Derived
  public String valueTitle() {
    return i18n().get("valueTitle", language());
  }

  @Value.Derived
  public String formatTitle() {
    return i18n().get("formatTitle", language());
  }

  @Value.Derived
  public String executeQueryButton() {
    return i18n().get("executeQueryButton", language());
  }

  @Value.Derived
  public String none() {
    return i18n().get("none", language());
  }
}
