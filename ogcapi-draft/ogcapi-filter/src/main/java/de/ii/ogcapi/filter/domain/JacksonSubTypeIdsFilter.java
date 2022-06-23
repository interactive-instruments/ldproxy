/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.filter.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.base.domain.JacksonSubTypeIds;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class JacksonSubTypeIdsFilter implements JacksonSubTypeIds {

  @Inject
  public JacksonSubTypeIdsFilter() {}

  @Override
  public Map<Class<?>, String> getMapping() {
    return new ImmutableMap.Builder<Class<?>, String>()
        .put(
            FilterConfiguration.class,
            ExtensionConfiguration.getBuildingBlockIdentifier(FilterConfiguration.class))
        .build();
  }
}
