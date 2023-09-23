/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.foundation.domain.ApiSecurity.PolicyAttribute;
import de.ii.xtraplatform.base.domain.util.Tuple;
import java.util.Map;
import java.util.Set;

@AutoMultiBind
public interface PolicyObligationFulfiller {
  boolean canFulfill(Map<String, PolicyAttribute> obligations, ApiOperation apiOperation);

  Tuple<ApiRequestContext, Set<String>> fulfill(
      Map<String, PolicyAttribute> obligations,
      ApiOperation apiOperation,
      ApiRequestContext requestContext,
      Map<String, String> values);
}
