/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.flatgeobuf.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableFlatgeobufConfiguration.Builder.class)
public interface FlatgeobufConfiguration extends ExtensionConfiguration, PropertyTransformations {

    /**
     * @return If the data is flattened and the feature schema includes arrays, {@code maxMultiplicity} properties
     * will be created for each array property. If an instance has more values in an array, only the first values
     * are included in the data.
     */
    @Nullable
    Integer getMaxMultiplicity();

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Override
    default Builder getBuilder() {
        return new ImmutableFlatgeobufConfiguration.Builder();
    }
}
