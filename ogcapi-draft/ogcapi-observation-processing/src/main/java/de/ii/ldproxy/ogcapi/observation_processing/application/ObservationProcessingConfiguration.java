/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformations;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.ProcessDocumentation;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.*;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableObservationProcessingConfiguration.Builder.class)
public interface ObservationProcessingConfiguration extends ExtensionConfiguration, FeatureTransformations {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    List<Variable> getVariables();

    List<Double> getDefaultBbox();

    Optional<String> getDefaultCoordPosition();

    Optional<String> getDefaultCoordArea();

    Optional<String> getDefaultDatetime();

    OptionalInt getDefaultWidth();

    Map<String, ProcessDocumentation> getDocumentation();

    List<String> getResultEncodings();

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

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableObservationProcessingConfiguration.Builder builder = ((ImmutableObservationProcessingConfiguration.Builder) source.getBuilder())
                .from(source)
                .from(this);

        //TODO: this is a work-around for default from behaviour (map is not reset, which leads to duplicates in ImmutableMap)
        // try to find a better solution that also enables deep merges
        if (getDocumentation()!=null)
            builder.documentation(getDocumentation());
        if (!getResultEncodings().isEmpty())
            builder.resultEncodings(getResultEncodings());

        return builder.build();
    }
}
