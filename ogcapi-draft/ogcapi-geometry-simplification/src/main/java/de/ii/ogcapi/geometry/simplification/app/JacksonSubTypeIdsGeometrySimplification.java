/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.geometry.simplification.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.base.domain.ImmutableJacksonSubType;
import de.ii.xtraplatform.base.domain.JacksonSubTypeIds;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class JacksonSubTypeIdsGeometrySimplification implements JacksonSubTypeIds {

  @Inject
  public JacksonSubTypeIdsGeometrySimplification() {}

  @Override
  public List<JacksonSubType> getSubTypes() {
    return ImmutableList.of(
        ImmutableJacksonSubType.builder()
            .superType(ExtensionConfiguration.class)
            .subType(GeometrySimplificationConfiguration.class)
            .id(ExtensionConfiguration.getBuildingBlockIdentifier(GeometrySimplificationConfiguration.class))
            .build());
  }
}
