/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.foundation.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = ImmutableExternalDocumentation.Builder.class)
public abstract class ExternalDocumentation {
    public abstract Optional<String> getDescription();
    public abstract String getUrl();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<ExternalDocumentation> FUNNEL = (from, into) -> {
        from.getDescription().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        into.putString(from.getUrl(), StandardCharsets.UTF_8);
    };
}
