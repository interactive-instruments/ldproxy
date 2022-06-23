/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain;

import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.foundation.domain.QueryIdentifier;
import de.ii.ogcapi.foundation.domain.QueryInput;
import java.util.Optional;
import org.immutables.value.Value;

public interface QueriesHandlerStyles extends QueriesHandler<QueriesHandlerStyles.Query> {

  enum Query implements QueryIdentifier {
    STYLES,
    STYLE,
    STYLE_METADATA
  }

  @Value.Immutable
  interface QueryInputStyles extends QueryInput {
    Optional<String> getCollectionId();

    boolean getIncludeLinkHeader();
  }

  @Value.Immutable
  interface QueryInputStyle extends QueryInput {
    Optional<String> getCollectionId();

    String getStyleId();

    boolean getIncludeLinkHeader();
  }
}
