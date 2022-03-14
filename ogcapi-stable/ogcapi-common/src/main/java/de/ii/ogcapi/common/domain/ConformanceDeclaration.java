/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import de.ii.ogcapi.foundation.domain.PageRepresentation;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Value.Immutable
@JsonDeserialize(builder = ImmutableConformanceDeclaration.Builder.class)
public abstract class ConformanceDeclaration extends PageRepresentation {

    public abstract List<String> getConformsTo();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<ConformanceDeclaration> FUNNEL = (from, into) -> {
        PageRepresentation.FUNNEL.funnel(from, into);
        from.getConformsTo()
            .stream()
            .sorted()
            .forEachOrdered(uri -> into.putString(uri, StandardCharsets.UTF_8));
        from.getExtensions()
            .keySet()
            .stream()
            .sorted()
            .forEachOrdered(key -> into.putString(key, StandardCharsets.UTF_8));
        // we cannot encode the generic extension object
    };
}
