/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableJsonSchemaRef.class)
public abstract class JsonSchemaRef extends JsonSchema {

    @JsonIgnore
    public final String getType() { return "$refDefs"; }

    @JsonProperty("$ref")
    @Value.Derived
    public String getRef() {
        return String.format("#/%s/%s", getDefsName(), getObjectType());
    }

    @JsonIgnore
    @Value.Auxiliary
    public String getDefsName() {
        return "$defs";
    }

    @JsonIgnore
    @Value.Auxiliary
    public abstract String getObjectType();

    @JsonIgnore
    @Nullable
    @Value.Auxiliary
    public abstract JsonSchema getDef();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<JsonSchemaRef> FUNNEL = (from, into) -> {
        into.putString(from.getType(), StandardCharsets.UTF_8);
        into.putString(from.getRef(), StandardCharsets.UTF_8);
    };
}
