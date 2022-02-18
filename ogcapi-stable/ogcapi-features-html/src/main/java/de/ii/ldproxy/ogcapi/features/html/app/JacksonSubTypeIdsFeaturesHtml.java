/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration;
import de.ii.ldproxy.ogcapi.features.html.domain.legacy.MicrodataGeometryMapping;
import de.ii.ldproxy.ogcapi.features.html.domain.legacy.MicrodataPropertyMapping;
import de.ii.xtraplatform.base.domain.JacksonSubTypeIds;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class JacksonSubTypeIdsFeaturesHtml implements JacksonSubTypeIds {

    @Inject
    JacksonSubTypeIdsFeaturesHtml() {
    }

    @Override
    public Map<Class<?>, String> getMapping() {
        return new ImmutableMap.Builder<Class<?>, String>()
                .put(MicrodataPropertyMapping.class, "MICRODATA_PROPERTY")
                .put(MicrodataGeometryMapping.class, "MICRODATA_GEOMETRY")
                .put(FeaturesHtmlConfiguration.class, ExtensionConfiguration.getBuildingBlockIdentifier(FeaturesHtmlConfiguration.class))
                .build();
    }
}
