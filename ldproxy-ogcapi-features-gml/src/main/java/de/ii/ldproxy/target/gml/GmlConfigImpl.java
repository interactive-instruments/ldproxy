/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import com.google.common.base.Strings;
import de.ii.xtraplatform.cfgstore.api.BundleConfigDefault;
import de.ii.xtraplatform.cfgstore.api.ConfigPropertyDescriptor;
import de.ii.xtraplatform.cfgstore.api.handler.LocalBundleConfig;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@LocalBundleConfig(bundleId = "ldproxy-target-gml", category = "GML Output Format", properties = {
        @ConfigPropertyDescriptor(name = GmlConfigImpl.ENABLED, label = "Enable GML/XML output format?", defaultValue = "true",uiType = ConfigPropertyDescriptor.UI_TYPE.CHECKBOX)
})
public class GmlConfigImpl extends BundleConfigDefault implements GmlConfig {

    static final String ENABLED = "enabled";

    @Override
    public boolean isEnabled() {
        return Strings.nullToEmpty(properties.get(ENABLED)).toLowerCase().equals("true");
    }
}
