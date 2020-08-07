/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.oas30;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiBuildingBlock;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;


@Component
@Provides
@Instantiate
public class OgcApiCapabilityOpenApi implements OgcApiBuildingBlock {

    @Override
    public ExtensionConfiguration.Builder getConfigurationBuilder() {
        return new ImmutableOas30Configuration.Builder();
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableOas30Configuration.Builder().enabled(true)
                                                        .build();
    }

}
