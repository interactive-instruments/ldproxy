/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyValueTransformer;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable(builder = true)
public interface FeaturePropertyTransformerNullValue extends FeaturePropertyValueTransformer {

    String TYPE = "NULL_VALUE";

    @Override
    default String getType() {
        return TYPE;
    }

    @Override
    default List<FeatureProperty.Type> getSupportedPropertyTypes() {
        return ImmutableList.of(FeatureProperty.Type.STRING, FeatureProperty.Type.INTEGER, FeatureProperty.Type.FLOAT);
    }

    @Override
    default String transform(String input) {
        if (input.matches(getParameter()))
            return null;

        return input;
    }
}
