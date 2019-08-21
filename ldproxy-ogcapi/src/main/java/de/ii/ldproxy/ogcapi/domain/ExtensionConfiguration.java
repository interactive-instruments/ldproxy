/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.google.common.base.CaseFormat;
import de.ii.xtraplatform.dropwizard.cfg.JacksonProvider;

/**
 * @author zahnen
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "extensionType")
@JsonTypeIdResolver(JacksonProvider.DynamicTypeIdResolver.class)
public interface ExtensionConfiguration {

    static String getExtensionType(Class<? extends ExtensionConfiguration> clazz) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, clazz.getSimpleName()
                                                                           .replace("Configuration", ""));
    }

    @JsonIgnore
    default String getExtensionType() {
        return getExtensionType((Class<? extends ExtensionConfiguration>) this.getClass()
                                                                              .getSuperclass());
    }

    boolean getEnabled();

    default <T extends ExtensionConfiguration> T mergeDefaults(T extensionConfigurationDefault) {
        return (T) this;
    }
}
