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
import java.util.Map;

@AutoMultiBind
public interface PolicyAttributeResolver {

  enum Category {
    SUBJECT,
    RESOURCE,
    ACTION
  }

  Category getCategory();

  boolean canResolve(Map<String, PolicyAttribute> attributes, ApiOperation apiOperation);

  Map<String, ?> resolve(
      Map<String, PolicyAttribute> attributes,
      ApiOperation apiOperation,
      ApiRequestContext requestContext);
}
