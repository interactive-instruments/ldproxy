/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableJsonSchemaString.class)
public abstract class JsonSchemaString extends JsonSchema {

    public final String getType() { return "string"; }

    public abstract Optional<String> getFormat();
    public abstract Optional<String> getPattern();
    @JsonProperty("enum")
    public abstract List<String> getEnums();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<JsonSchemaString> FUNNEL = (from, into) -> {
        into.putString(from.getType(), StandardCharsets.UTF_8);
        from.getFormat().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getPattern().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getEnums()
            .stream()
            .sorted()
            .forEachOrdered(val -> into.putString(val, StandardCharsets.UTF_8));
    };
}
