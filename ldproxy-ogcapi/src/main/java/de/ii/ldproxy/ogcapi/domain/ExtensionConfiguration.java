/**
 * Copyright 2020 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.google.common.base.CaseFormat;
import de.ii.xtraplatform.dropwizard.cfg.JacksonProvider;


@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "buildingBlock")
@JsonTypeIdResolver(JacksonProvider.DynamicTypeIdResolver.class)
public interface ExtensionConfiguration {

    static String getBuildingBlockIdentifier(Class<? extends ExtensionConfiguration> clazz) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, clazz.getSimpleName()
                                                                           .replace("Configuration", ""));
    }

    @JsonAlias("extensionType")
    default String getBuildingBlock() {
        return getBuildingBlockIdentifier((Class<? extends ExtensionConfiguration>) this.getClass()
                                                                                         .getSuperclass());
    }

    boolean getEnabled();

    default <T extends ExtensionConfiguration> T mergeDefaults(T extensionConfigurationDefault) {
        return null;
    }
}
