/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import java.util.List;
import java.util.Map;

public interface ApiSecurityInfo {
  List<PermissionGroup> getActiveGroups(OgcApiDataV2 apiData);

  Map<String, String> getActiveScopes(OgcApiDataV2 apiData);
}
