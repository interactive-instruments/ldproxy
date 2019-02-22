/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.generalization;

import de.ii.ldproxy.wfs3.api.ExtensionConfiguration;
import de.ii.ldproxy.wfs3.api.Wfs3CapabilityExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3CapabilityGeneralization implements Wfs3CapabilityExtension {
    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return ImmutableGeneralizationConfiguration.builder().build();
    }
}
