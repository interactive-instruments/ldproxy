/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformations;
import de.ii.ldproxy.ogcapi.features.processing.ProcessDocumentation;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableObservationProcessingConfiguration.Builder.class)
public abstract class ObservationProcessingConfiguration implements ExtensionConfiguration, FeatureTransformations {

    // TODO this module belongs to ogcapi-experimental, not ogcapi-draft, but it is part of ogcapi-draft due
    //      to the HTML module dependency issues

    @Value.Default
    @Override
    public boolean getEnabled() {
        return false;
    }

    public abstract List<Variable> getVariables();

    public abstract List<Double> getDefaultBbox();
    public abstract Optional<String> getDefaultCoordPosition();
    public abstract Optional<String> getDefaultCoordArea();
    public abstract Optional<String> getDefaultDatetime();
    public abstract OptionalInt getDefaultWidth();
    public abstract Map<String, ProcessDocumentation> getDocumentation();

    @Override
    public <T extends ExtensionConfiguration> T mergeDefaults(T extensionConfigurationDefault) {

        return (T) new ImmutableObservationProcessingConfiguration.Builder().from(extensionConfigurationDefault)
                                                                         .from(this)
                                                                         .build();
    }
}
