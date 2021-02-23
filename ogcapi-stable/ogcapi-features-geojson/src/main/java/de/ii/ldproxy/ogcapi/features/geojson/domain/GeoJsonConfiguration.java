/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformerBase;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformations;
import de.ii.ldproxy.ogcapi.features.core.domain.PropertyTransformation;
import java.util.LinkedHashMap;
import java.util.Map;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableGeoJsonConfiguration.Builder.class)
public interface GeoJsonConfiguration extends ExtensionConfiguration, FeatureTransformations {

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Nullable
    FeatureTransformerBase.NESTED_OBJECTS getNestedObjectStrategy();

    @Nullable
    FeatureTransformerBase.MULTIPLICITY getMultiplicityStrategy();

    @Nullable
    Boolean getUseFormattedJsonOutput();

    @Nullable
    String getSeparator();

    @Override
    default Builder getBuilder() {
        return new ImmutableGeoJsonConfiguration.Builder();
    }

    @Override
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableGeoJsonConfiguration.Builder builder = new ImmutableGeoJsonConfiguration.Builder()
            .from(source)
            .from(this);

        Map<String, PropertyTransformation> mergedTransformations = new LinkedHashMap<>(
            ((GeoJsonConfiguration) source).getTransformations());
        getTransformations().forEach((key, transformation) -> {
            if (mergedTransformations.containsKey(key)) {
                mergedTransformations.put(key, transformation.mergeInto(mergedTransformations.get(key)));
            } else {
                mergedTransformations.put(key, transformation);
            }
        });
        builder.transformations(mergedTransformations);

        return builder.build();
    }
}
