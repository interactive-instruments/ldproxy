/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableJsonSchemaTrue.class)
public abstract class JsonSchemaTrue extends JsonSchema {

    // any instance is valid, same as {}

    public final String getType() { return "true"; }

    @JsonValue
    public final boolean toValue() { return true; }

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<JsonSchemaTrue> FUNNEL = (from, into) -> {
        into.putString(from.getType(), StandardCharsets.UTF_8);
    };
}
