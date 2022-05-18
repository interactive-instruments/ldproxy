/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.hash.Funnel;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public abstract class JsonSchema {

    public abstract Optional<String> getTitle();

    public abstract Optional<String> getDescription();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<JsonSchema> FUNNEL = (from, into) -> {
        from.getTitle().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getDescription().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        if (from instanceof JsonSchemaString)
            JsonSchemaString.FUNNEL.funnel((JsonSchemaString) from, into);
        else if (from instanceof JsonSchemaNumber)
            JsonSchemaNumber.FUNNEL.funnel((JsonSchemaNumber) from, into);
        else if (from instanceof JsonSchemaInteger)
            JsonSchemaInteger.FUNNEL.funnel((JsonSchemaInteger) from, into);
        else if (from instanceof JsonSchemaBoolean)
            JsonSchemaBoolean.FUNNEL.funnel((JsonSchemaBoolean) from, into);
        else if (from instanceof JsonSchemaObject)
            JsonSchemaObject.FUNNEL.funnel((JsonSchemaObject) from, into);
        else if (from instanceof JsonSchemaArray)
            JsonSchemaArray.FUNNEL.funnel((JsonSchemaArray) from, into);
        else if (from instanceof JsonSchemaNull)
            JsonSchemaNull.FUNNEL.funnel((JsonSchemaNull) from, into);
        else if (from instanceof JsonSchemaTrue)
            JsonSchemaTrue.FUNNEL.funnel((JsonSchemaTrue) from, into);
        else if (from instanceof JsonSchemaFalse)
            JsonSchemaFalse.FUNNEL.funnel((JsonSchemaFalse) from, into);
        else if (from instanceof JsonSchemaRef)
            JsonSchemaRef.FUNNEL.funnel((JsonSchemaRef) from, into);
        else if (from instanceof JsonSchemaOneOf)
            JsonSchemaOneOf.FUNNEL.funnel((JsonSchemaOneOf) from, into);
    };

    @JsonIgnore
    @Value.Auxiliary
    public abstract Optional<String> getName();

    @JsonIgnore
    @Value.Default
    @Value.Auxiliary
    public boolean isRequired() {
        return false;
    }
}
