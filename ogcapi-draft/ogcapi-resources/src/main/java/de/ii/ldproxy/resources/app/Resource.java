/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.PageRepresentation;
import de.ii.ldproxy.ogcapi.styles.domain.StylesheetMetadata;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Objects;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(as = ImmutableResource.class)
public abstract class Resource {

    public abstract String getId();

    public abstract Link getLink();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<Resource> FUNNEL = (from, into) -> {
        into.putString(from.getId(), StandardCharsets.UTF_8);
        into.putString(from.getLink().getHref(), StandardCharsets.UTF_8)
            .putString(Objects.requireNonNullElse(from.getLink().getRel(), ""), StandardCharsets.UTF_8);
    };
}
