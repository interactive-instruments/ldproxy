/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.domain;

import de.ii.ogcapi.common.app.QueriesHandlerCommonImpl;
import de.ii.ogcapi.foundation.domain.PermissionGroup;
import de.ii.ogcapi.foundation.domain.PermissionGroup.Base;
import de.ii.ogcapi.foundation.domain.QueriesHandler;

public interface QueriesHandlerCommon extends QueriesHandler<QueriesHandlerCommonImpl.Query> {

  String GROUP_COLLECTIONS = "collections";
  PermissionGroup GROUP_COLLECTIONS_READ =
      PermissionGroup.of(Base.READ, GROUP_COLLECTIONS, "access feature collection metadata");
}
