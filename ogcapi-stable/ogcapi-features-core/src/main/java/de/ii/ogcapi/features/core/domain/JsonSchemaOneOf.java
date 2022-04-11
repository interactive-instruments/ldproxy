/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.google.common.hash.Funnel;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
public abstract class JsonSchemaOneOf extends JsonSchema {

    public abstract List<JsonSchema> getOneOf();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<JsonSchemaOneOf> FUNNEL = (from, into) -> {
        from.getOneOf()
            .stream()
            .forEachOrdered(val -> JsonSchema.FUNNEL.funnel(val, into));
    };
}
