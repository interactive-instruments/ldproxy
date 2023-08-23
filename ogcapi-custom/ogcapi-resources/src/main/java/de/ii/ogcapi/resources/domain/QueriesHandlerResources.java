/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.domain;

import de.ii.ogcapi.foundation.domain.ApiSecurity.Scope;
import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.foundation.domain.QueryIdentifier;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.xtraplatform.base.domain.util.Tuple;
import org.immutables.value.Value;

public interface QueriesHandlerResources extends QueriesHandler<QueriesHandlerResources.Query> {

  String SCOPE_RESOURCES = "resources";
  Tuple<Scope, String> SCOPE_RESOURCES_READ = Tuple.of(Scope.READ, SCOPE_RESOURCES);
  Tuple<Scope, String> SCOPE_RESOURCES_WRITE = Tuple.of(Scope.WRITE, SCOPE_RESOURCES);

  enum Query implements QueryIdentifier {
    RESOURCES,
    RESOURCE
  }

  @Value.Immutable
  interface QueryInputResources extends QueryInput {
    boolean getIncludeLinkHeader();
  }

  @Value.Immutable
  interface QueryInputResource extends QueryInput {
    String getResourceId();
  }
}
