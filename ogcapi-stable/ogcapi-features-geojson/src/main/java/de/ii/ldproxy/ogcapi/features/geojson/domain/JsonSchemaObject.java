/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableJsonSchemaObject.class)
public abstract class JsonSchemaObject extends JsonSchema {

    public final String getType() { return "object"; }

    public abstract List<String> getRequired();
    public abstract Map<String, JsonSchema> getProperties();
    public abstract Map<String, JsonSchema> getPatternProperties();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<JsonSchemaObject> FUNNEL = (from, into) -> {
        into.putString(from.getType(), StandardCharsets.UTF_8);
        from.getRequired()
            .stream()
            .sorted()
            .forEachOrdered(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getProperties()
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(entry -> JsonSchema.FUNNEL.funnel(entry.getValue(), into));
        from.getPatternProperties()
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(entry -> JsonSchema.FUNNEL.funnel(entry.getValue(), into));
    };
}
