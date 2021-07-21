/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableJsonSchemaRef.class)
public abstract class JsonSchemaRef extends JsonSchema {

    @JsonIgnore
    public final String getType() { return "$ref"; }

    @JsonProperty("$ref")
    public abstract String getRef();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<JsonSchemaRef> FUNNEL = (from, into) -> {
        into.putString(from.getType(), StandardCharsets.UTF_8);
        into.putString(from.getRef(), StandardCharsets.UTF_8);
    };
}
