/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain;

import de.ii.ldproxy.ogcapi.common.app.QueriesHandlerCommonImpl;
import de.ii.ldproxy.ogcapi.domain.QueriesHandler;

public interface QueriesHandlerCommon extends QueriesHandler<QueriesHandlerCommonImpl.Query> {
}
