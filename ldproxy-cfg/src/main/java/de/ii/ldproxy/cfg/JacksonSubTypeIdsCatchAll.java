/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.base.domain.ImmutableJacksonSubType;
import de.ii.xtraplatform.base.domain.JacksonSubTypeIds;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableBuilder;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

class JacksonSubTypeIdsCatchAll implements JacksonSubTypeIds {

  @Override
  public List<JacksonSubType> getSubTypes() {
    return ImmutableList.of(
        ImmutableJacksonSubType.builder()
            .superType(ExtensionConfiguration.class)
            .subType(CatchAllConfiguration.class)
            .id(".+")
            .build());
  }

  public static class CatchAllConfiguration implements ExtensionConfiguration {

    @Nullable
    @Override
    public Boolean getEnabled() {
      return null;
    }

    @Override
    public Optional<ExtensionConfiguration> getDefaultValues() {
      return Optional.empty();
    }

    abstract class Builder extends ExtensionConfiguration.Builder {}

    @Override
    public ExtensionConfiguration.Builder getBuilder() {
      return new ExtensionConfiguration.Builder() {
        @Override
        public <U extends BuildableBuilder<ExtensionConfiguration>> U from(
            ExtensionConfiguration value) {
          return (U) this;
        }

        @Override
        public ExtensionConfiguration build() {
          return new CatchAllConfiguration();
        }

        @Override
        public ExtensionConfiguration.Builder defaultValues(ExtensionConfiguration defaultValues) {
          return this;
        }
      };
    }
  }
}
