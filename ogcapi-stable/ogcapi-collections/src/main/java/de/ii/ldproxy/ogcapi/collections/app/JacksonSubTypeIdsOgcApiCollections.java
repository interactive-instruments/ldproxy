/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.base.domain.JacksonSubTypeIds;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

@Singleton
@AutoBind
public class JacksonSubTypeIdsOgcApiCollections implements JacksonSubTypeIds {

    @Inject
    public JacksonSubTypeIdsOgcApiCollections() {
    }

    @Override
    public Map<Class<?>, String> getMapping() {
        return new ImmutableMap.Builder<Class<?>, String>()
                .put(CollectionsConfiguration.class, ExtensionConfiguration.getBuildingBlockIdentifier(CollectionsConfiguration.class))
                .build();
    }
}
