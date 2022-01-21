/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import de.ii.ldproxy.ogcapi.domain.ExternalDocumentation;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.PageRepresentation;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@JsonDeserialize(builder = ImmutableLandingPage.Builder.class)
public abstract class LandingPage extends PageRepresentation {

    public abstract Optional<String> getAttribution();

    public abstract Optional<OgcApiExtent> getExtent();

    public abstract Optional<ExternalDocumentation> getExternalDocs();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<LandingPage> FUNNEL = (from, into) -> {
        PageRepresentation.FUNNEL.funnel(from, into);
        from.getAttribution().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getExtent().ifPresent(val -> OgcApiExtent.FUNNEL.funnel(val, into));
        from.getExternalDocs().ifPresent(val -> ExternalDocumentation.FUNNEL.funnel(val, into));
        from.getExtensions()
            .keySet()
            .stream()
            .sorted()
            .forEachOrdered(key -> into.putString(key, StandardCharsets.UTF_8));
        // we cannot encode the generic extension object
    };

}
