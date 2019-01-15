/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import de.ii.xsf.dropwizard.cfg.JacksonProvider;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "extensionType")
@JsonTypeIdResolver(JacksonProvider.DynamicTypeIdResolver.class)
public interface ExtensionConfiguration {
    boolean getEnabled();
    ExtensionConfiguration mergeDefaults(ExtensionConfiguration extensionConfigurationDefault);
}
