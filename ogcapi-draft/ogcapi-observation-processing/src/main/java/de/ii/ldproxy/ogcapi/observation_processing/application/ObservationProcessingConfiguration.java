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

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableObservationProcessingConfiguration.Builder.class)
public interface ObservationProcessingConfiguration extends ExtensionConfiguration, FeatureTransformations {

    // TODO this module belongs to ogcapi-experimental, not ogcapi-draft, but it is part of ogcapi-draft due
    //      to the HTML module dependency issues

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    List<Variable> getVariables();

    List<Double> getDefaultBbox();

    Optional<String> getDefaultCoordPosition();

    Optional<String> getDefaultCoordArea();

    Optional<String> getDefaultDatetime();

    OptionalInt getDefaultWidth();

    Map<String, ProcessDocumentation> getDocumentation();

    @Nullable
    Double getIdwPower();

    @Nullable
    Integer getIdwCount();

    @Nullable
    Double getIdwDistanceKm();

    @Override
    default Builder getBuilder() {
        return new ImmutableObservationProcessingConfiguration.Builder();
    }

    //TODO: this is a work-around for default from behaviour (map is not reset, which leads to duplicates in ImmutableMap)
    // try to find a better solution that also enables deep merges
    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        return ((ImmutableObservationProcessingConfiguration.Builder) source.getBuilder())
                .from(source)
                .from(this)
                .documentation(getDocumentation())
                .build();
    }

}
