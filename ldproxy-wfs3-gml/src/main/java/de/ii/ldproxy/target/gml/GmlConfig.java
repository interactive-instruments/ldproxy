/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import com.google.common.base.Strings;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {GmlConfig.class})
@Instantiate
/*@LocalBundleConfig(bundleId = "ldproxy-target-gml", category = "GML Output Format", properties = {
        @ConfigPropertyDescriptor(name = ENABLED, label = "Enable GML/XML output format?", defaultValue = "true",uiType = ConfigPropertyDescriptor.UI_TYPE.CHECKBOX)
})*/
public class GmlConfig /*extends BundleConfigDefault*/ {

    static final String ENABLED = "enabled";

    Map<String, String> properties = new HashMap<>();

    public boolean isEnabled() {
        return Strings.nullToEmpty(properties.get(ENABLED)).toLowerCase().equals("true");
    }
}
