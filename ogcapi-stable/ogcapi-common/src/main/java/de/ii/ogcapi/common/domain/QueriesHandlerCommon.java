/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.domain;

import de.ii.ogcapi.common.app.QueriesHandlerCommonImpl;
import de.ii.ogcapi.foundation.domain.ApiSecurity.Scope;
import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.xtraplatform.base.domain.util.Tuple;

public interface QueriesHandlerCommon extends QueriesHandler<QueriesHandlerCommonImpl.Query> {

  String SCOPE_COLLECTIONS = "collections";
  Tuple<Scope, String> SCOPE_COLLECTIONS_READ = Tuple.of(Scope.READ, SCOPE_COLLECTIONS);
}
