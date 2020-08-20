/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.google.common.collect.ImmutableList;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate
public class OgcApiCapabilityCommon implements OgcApiBuildingBlock {

    @Requires
    OgcApiExtensionRegistry extensionRegistry;

    @Override
    public ExtensionConfiguration.Builder getConfigurationBuilder() {
        return new ImmutableOgcApiCommonConfiguration.Builder();
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableOgcApiCommonConfiguration.Builder().enabled(true)
                                                               .includeHomeLink(false)
                                                               .includeLinkHeader(true)
                                                               .useLangParameter(false)
                                                               .encodings(extensionRegistry.getExtensionsForType(CommonFormatExtension.class)
                                                                                           .stream()
                                                                                           .filter(FormatExtension::isEnabledByDefault)
                                                                                           .map(format -> format.getMediaType().label())
                                                                                           .collect(ImmutableList.toImmutableList()))
                                                               .build();
    }

}
