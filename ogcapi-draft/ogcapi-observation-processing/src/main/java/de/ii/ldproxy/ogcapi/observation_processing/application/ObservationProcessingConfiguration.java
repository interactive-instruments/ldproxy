/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformations;
import de.ii.ldproxy.ogcapi.features.core.domain.PropertyTransformation;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.ProcessDocumentation;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.*;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableObservationProcessingConfiguration.Builder.class)
public interface ObservationProcessingConfiguration extends ExtensionConfiguration, FeatureTransformations {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @JsonMerge(OptBoolean.FALSE)
    List<Variable> getVariables();

    @JsonMerge(OptBoolean.FALSE)
    List<Double> getDefaultBbox();

    Optional<String> getDefaultCoordPosition();

    Optional<String> getDefaultCoordArea();

    Optional<String> getDefaultDatetime();

    OptionalInt getDefaultWidth();

    Map<String, ProcessDocumentation> getDocumentation();

    @JsonMerge(OptBoolean.FALSE)
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

        Map<String, PropertyTransformation> mergedTransformations = new LinkedHashMap<>(
                ((ObservationProcessingConfiguration) source).getTransformations());
        getTransformations().forEach((key, transformation) -> {
            if (mergedTransformations.containsKey(key)) {
                mergedTransformations.put(key, transformation.mergeInto(mergedTransformations.get(key)));
            } else {
                mergedTransformations.put(key, transformation);
            }
        });
        builder.transformations(mergedTransformations);

        Map<String, ProcessDocumentation> mergedDocumentation = new LinkedHashMap<>(
                ((ObservationProcessingConfiguration) source).getDocumentation());
        getDocumentation().forEach(mergedDocumentation::putIfAbsent);
        builder.documentation(mergedDocumentation);

        if (!getDefaultBbox().isEmpty()) {
            builder.defaultBbox(getDefaultBbox());
        }

        List<Variable> variables = Lists.newArrayList(((ObservationProcessingConfiguration) source).getVariables());
        getVariables().forEach(variable -> {
            if (!variables.contains(variable)) {
                variables.add(variable);
            }
        });
        builder.variables(variables);

        List<String> resultEncodings = Lists.newArrayList(((ObservationProcessingConfiguration) source).getResultEncodings());
        getResultEncodings().forEach(resultEncoding -> {
            if (!resultEncodings.contains(resultEncoding)) {
                resultEncodings.add(resultEncoding);
            }
        });
        builder.resultEncodings(resultEncodings);

        return builder.build();
    }
}
