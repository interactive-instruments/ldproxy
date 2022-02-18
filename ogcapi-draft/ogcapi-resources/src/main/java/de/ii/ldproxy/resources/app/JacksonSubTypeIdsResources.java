/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources.app;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.resources.domain.ResourcesConfiguration;
import de.ii.xtraplatform.dropwizard.domain.JacksonSubTypeIds;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import java.util.Map;

@Singleton
@AutoBind
public class JacksonSubTypeIdsResources implements JacksonSubTypeIds {
    @Override
    public Map<Class<?>, String> getMapping() {
        return new ImmutableMap.Builder<Class<?>, String>()
                .put(ResourcesConfiguration.class, ExtensionConfiguration.getBuildingBlockIdentifier(ResourcesConfiguration.class))
                .build();
    }
}
