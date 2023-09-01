/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.text.search.app;

import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.text.search.domain.ImmutableTextSearchConfiguration;
import javax.inject.Inject;

/**
 * @title Text Search
 * @langEn General text search in multiple text properties of the data.
 * @langDe Generelle Textsuche über mehrere Properties der Daten.
 * @conformanceEn *Text Search* implements requirements of the section "Parameter q" of the [draft
 *     OGC API - Records - Part 1:
 *     Core](https://docs.ogc.org/DRAFTS/20-004.html#core-query-parameters-q).
 * @conformanceDe Das Modul implementiert die Anforderungen des Abschnitts "Parameter q" des
 *     [Entwurfs von OGC API - Records - Part 1:
 *     Core](https://docs.ogc.org/DRAFTS/20-004.html#core-query-parameters-q).
 * @ref:cfg {@link de.ii.ogcapi.text.search.domain.TextSearchConfiguration}
 * @ref:cfgProperties {@link de.ii.ogcapi.text.search.domain.ImmutableTextSearchConfiguration}
 * @ref:queryParameters {@link de.ii.ogcapi.text.search.app.QueryParameterQ}
 */
public class TextSearchBuildingBlock implements ApiBuildingBlock {
  @Inject
  public TextSearchBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableTextSearchConfiguration.Builder().enabled(false).build();
  }
}
