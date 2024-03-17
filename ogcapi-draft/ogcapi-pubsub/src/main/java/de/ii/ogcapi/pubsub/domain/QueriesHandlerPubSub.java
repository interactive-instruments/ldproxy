/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.domain;

import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.foundation.domain.QueryIdentifier;
import de.ii.xtraplatform.base.domain.resiliency.Volatile2;

public interface QueriesHandlerPubSub
    extends QueriesHandler<QueriesHandlerPubSub.Query>, Volatile2 {

  enum Query implements QueryIdentifier {
    ASYNC_API_DEFINITION
  }
}
