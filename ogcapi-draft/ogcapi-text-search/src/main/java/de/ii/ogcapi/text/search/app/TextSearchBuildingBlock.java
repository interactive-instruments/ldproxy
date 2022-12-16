/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.text.search.app;

import de.ii.ogcapi.collections.domain.ImmutableCollectionsConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;

/**
 * @title Text Search
 * @langEn General text search in multiple text properties of the data.
 * @langDe Generelle Textsuche über mehrere Properties der Daten.
 * @ref:cfgProperties {@link de.ii.ogcapi.text.search.domain.ImmutableTextSearchConfiguration}
 * @ref:queryParameters {@link de.ii.ogcapi.text.search.app.QueryParameterQ}
 */
public class TextSearchBuildingBlock implements ApiBuildingBlock {
  @Inject
  public TextSearchBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableCollectionsConfiguration.Builder().enabled(false).build();
  }
}
