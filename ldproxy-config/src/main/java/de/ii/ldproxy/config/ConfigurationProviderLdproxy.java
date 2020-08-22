/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.config;

import de.ii.xtraplatform.dropwizard.domain.AbstractConfigurationProvider;
import de.ii.xtraplatform.runtime.domain.XtraPlatformConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides
@Instantiate
public class ConfigurationProviderLdproxy extends AbstractConfigurationProvider<XtraPlatformConfiguration> {

    @Override
    public Class<XtraPlatformConfiguration> getConfigurationClass() {
        return XtraPlatformConfiguration.class;
    }
}
