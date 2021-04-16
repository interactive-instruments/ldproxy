/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import de.ii.ldproxy.ogcapi.domain.AbstractExtensionConfigurationSpec
import de.ii.ldproxy.ogcapi.domain.MergeBase
import de.ii.ldproxy.ogcapi.domain.MergeCollection
import de.ii.ldproxy.ogcapi.domain.MergeMap
import de.ii.ldproxy.ogcapi.domain.MergeMinimal
import de.ii.ldproxy.ogcapi.domain.MergeNested
import de.ii.ldproxy.ogcapi.domain.MergeSimple
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutablePropertyTransformation
import de.ii.ldproxy.ogcapi.features.core.domain.processing.ImmutableProcessDocumentation

@SuppressWarnings('ClashingTraitMethods')
class ObservationProcessingConfigurationSpec extends AbstractExtensionConfigurationSpec implements MergeBase<ObservationProcessingConfiguration>, MergeMinimal<ObservationProcessingConfiguration>, MergeSimple<ObservationProcessingConfiguration>, MergeCollection<ObservationProcessingConfiguration>, MergeMap<ObservationProcessingConfiguration>, MergeNested<ObservationProcessingConfiguration> {

    @Override
    ObservationProcessingConfiguration getFull() {
        return new ImmutableObservationProcessingConfiguration.Builder()
                .enabled(true)
                .defaultCoordPosition("foo")
                .defaultCoordArea("foo")
                .defaultDatetime("foo")
                .defaultWidth(1)
                .idwPower(1)
                .idwCount(1)
                .idwDistanceKm(1)
                .addVariables(ImmutableVariable.builder().id("foo").build())
                .addDefaultBbox(1)
                .addResultEncodings("foo")
                .putDocumentation("foo", ImmutableProcessDocumentation.builder().summary("foo").build())
                .putTransformations("foo", new ImmutablePropertyTransformation.Builder().rename("bar").build())
                .build()
    }

    @Override
    ObservationProcessingConfiguration getMinimal() {
        return new ImmutableObservationProcessingConfiguration.Builder()
                .build()
    }

    @Override
    ObservationProcessingConfiguration getMinimalFullMerged() {
        return getFull()
    }

    @Override
    ObservationProcessingConfiguration getSimple() {
        return new ImmutableObservationProcessingConfiguration.Builder()
                .enabled(false)
                .defaultCoordPosition("bar")
                .defaultCoordArea("bar")
                .defaultDatetime("bar")
                .defaultWidth(2)
                .idwPower(2)
                .idwCount(2)
                .idwDistanceKm(2)
                .build()
    }

    @Override
    ObservationProcessingConfiguration getSimpleFullMerged() {
        return new ImmutableObservationProcessingConfiguration.Builder()
                .from(getFull())
                .from(getSimple())
                .build()
    }

    @Override
    ObservationProcessingConfiguration getCollection() {
        return new ImmutableObservationProcessingConfiguration.Builder()
                .addVariables(ImmutableVariable.builder().id("bar").build())
                .addDefaultBbox(2)
                .addResultEncodings("bar")
                .build()
    }

    @Override
    ObservationProcessingConfiguration getCollectionFullMerged() {
        return new ImmutableObservationProcessingConfiguration.Builder()
                .from(getFull())
                .variables(ImmutableList.of(
                        ImmutableVariable.builder().id("foo").build(),
                        ImmutableVariable.builder().id("bar").build()
                ))
                .defaultBbox(ImmutableList.of(Double.valueOf(2)))
                .resultEncodings(ImmutableList.of("foo", "bar"))
                .build()
    }

    @Override
    ObservationProcessingConfiguration getMap() {
        return new ImmutableObservationProcessingConfiguration.Builder()
                .putDocumentation("bar", ImmutableProcessDocumentation.builder().summary("bar").build())
                .putTransformations("bar", new ImmutablePropertyTransformation.Builder().rename("foo").build())
                .build()
    }

    @Override
    ObservationProcessingConfiguration getMapFullMerged() {
        return new ImmutableObservationProcessingConfiguration.Builder()
                .from(getFull())
                .documentation(ImmutableMap.of(
                        "foo", ImmutableProcessDocumentation.builder().summary("foo").build(),
                        "bar", ImmutableProcessDocumentation.builder().summary("bar").build()
                ))
                .transformations(ImmutableMap.of(
                        "foo", new ImmutablePropertyTransformation.Builder().rename("bar").build(),
                        "bar", new ImmutablePropertyTransformation.Builder().rename("foo").build()
                ))
                .build()
    }

    @Override
    ObservationProcessingConfiguration getNested() {
        return new ImmutableObservationProcessingConfiguration.Builder()
                .putTransformations("foo", new ImmutablePropertyTransformation.Builder().codelist("cl").build())
                .build()
    }

    @Override
    ObservationProcessingConfiguration getNestedFullMerged() {
        return new ImmutableObservationProcessingConfiguration.Builder()
                .from(getFull())
                .transformations(ImmutableMap.of(
                        "foo", new ImmutablePropertyTransformation.Builder().rename("bar").codelist("cl").build()
                ))
                .build()
    }
}
