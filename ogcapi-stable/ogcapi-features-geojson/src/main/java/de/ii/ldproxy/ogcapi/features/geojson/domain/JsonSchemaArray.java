/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableJsonSchemaArray.class)
public abstract class JsonSchemaArray extends JsonSchema {

    public final String getType() { return "array"; }

    public abstract JsonSchema getItems();
    public abstract Optional<Integer> getMinItems();
    public abstract Optional<Integer> getMaxItems();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<JsonSchemaArray> FUNNEL = (from, into) -> {
        into.putString(from.getType(), StandardCharsets.UTF_8);
        JsonSchema.FUNNEL.funnel(from.getItems(), into);
        from.getMinItems().ifPresent(val -> into.putInt(val));
        from.getMaxItems().ifPresent(val -> into.putInt(val));
    };
}
