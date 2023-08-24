/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain;

import de.ii.ogcapi.foundation.domain.ApiSecurity.Scope;
import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.foundation.domain.QueryIdentifier;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.xtraplatform.base.domain.util.Tuple;
import java.util.Optional;
import org.immutables.value.Value;

public interface QueriesHandlerStyles extends QueriesHandler<QueriesHandlerStyles.Query> {

  String SCOPE_STYLES = "styles";
  Tuple<Scope, String> SCOPE_STYLES_READ = Tuple.of(Scope.READ, SCOPE_STYLES);
  Tuple<Scope, String> SCOPE_STYLES_WRITE = Tuple.of(Scope.WRITE, SCOPE_STYLES);

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
