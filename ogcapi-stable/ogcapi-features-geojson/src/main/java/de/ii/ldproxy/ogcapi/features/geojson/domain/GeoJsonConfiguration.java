/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformerBase;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformerBase.MULTIPLICITY;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformerBase.NESTED_OBJECTS;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableGeoJsonConfiguration.Builder.class)
public interface GeoJsonConfiguration extends ExtensionConfiguration, PropertyTransformations {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Deprecated(since = "3.1.0")
    @Nullable
    FeatureTransformerBase.NESTED_OBJECTS getNestedObjectStrategy();

    @Deprecated(since = "3.1.0")
    @Nullable
    FeatureTransformerBase.MULTIPLICITY getMultiplicityStrategy();

    @Deprecated(since = "3.1.0")
    @Nullable
    Boolean getUseFormattedJsonOutput();

    @Deprecated(since = "3.1.0")
    @Nullable
    String getSeparator();

    @Nullable
    Boolean includeEmptyObjects();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isFlattened() {
        return getTransformations().containsKey(PropertyTransformations.WILDCARD)
            && getTransformations().get(PropertyTransformations.WILDCARD).getFlatten().isPresent();
    }

    @Value.Check
    default GeoJsonConfiguration backwardsCompatibility() {
        if (getNestedObjectStrategy() == NESTED_OBJECTS.FLATTEN
            && getMultiplicityStrategy() == MULTIPLICITY.SUFFIX
            && (!getTransformations().containsKey(PropertyTransformations.WILDCARD)
            || getTransformations().get(PropertyTransformations.WILDCARD).getFlatten().isEmpty())) {

            Map<String, PropertyTransformation> transformations = new LinkedHashMap<>(getTransformations());

            PropertyTransformation transformation = new ImmutablePropertyTransformation.Builder()
                .flatten(Optional.ofNullable(getSeparator()).orElse("."))
                .build();
            if (transformations.containsKey(PropertyTransformations.WILDCARD)) {
                transformations.put(PropertyTransformations.WILDCARD, transformation.mergeInto(transformations.get(PropertyTransformations.WILDCARD)));
            } else {
                transformations.put(PropertyTransformations.WILDCARD, transformation);
            }

            return new ImmutableGeoJsonConfiguration.Builder()
                .from(this)
                .transformations(transformations)
                .build();
        }

        return this;
    }

    @Override
    default Builder getBuilder() {
        return new ImmutableGeoJsonConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        return new ImmutableGeoJsonConfiguration.Builder()
            .from(source)
            .from(this)
            .transformations(PropertyTransformations.super.mergeInto((PropertyTransformations) source).getTransformations())
            .build();
    }
}
