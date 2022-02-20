/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableJsonSchemaBoolean.class)
public abstract class JsonSchemaBoolean extends JsonSchema {

    public final String getType() { return "boolean"; }

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<JsonSchemaBoolean> FUNNEL = (from, into) -> {
        into.putString(from.getType(), StandardCharsets.UTF_8);
    };
}
