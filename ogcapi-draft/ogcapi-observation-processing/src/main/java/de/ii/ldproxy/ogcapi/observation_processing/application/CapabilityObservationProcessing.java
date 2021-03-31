/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.observation_processing.api.DapaResultFormatExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate

public class CapabilityObservationProcessing implements ApiBuildingBlock {

    private final ExtensionRegistry extensionRegistry;

    public CapabilityObservationProcessing(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public ExtensionConfiguration.Builder getConfigurationBuilder() {
        return new ImmutableObservationProcessingConfiguration.Builder();
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableObservationProcessingConfiguration.Builder().enabled(false)
                                                                        .resultEncodings(extensionRegistry.getExtensionsForType(DapaResultFormatExtension.class)
                                                                                                        .stream()
                                                                                                        .filter(FormatExtension::isEnabledByDefault)
                                                                                                        .map(format -> format.getMediaType().label())
                                                                                                        .collect(ImmutableList.toImmutableList()))
                                                                        .idwPower(3.0)
                                                                        .idwCount(8)
                                                                        .idwDistanceKm(300.0)
                                                                        .build();
    }
}
