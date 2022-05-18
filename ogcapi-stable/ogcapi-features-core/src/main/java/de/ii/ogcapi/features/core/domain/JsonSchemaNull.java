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

import java.nio.charset.StandardCharsets;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
public abstract class JsonSchemaNull extends JsonSchema {

    public final String getType() { return "null"; }

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<JsonSchemaNull> FUNNEL = (from, into) -> {
        into.putString(from.getType(), StandardCharsets.UTF_8);
    };
}
