/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import de.ii.ldproxy.ogcapi.common.domain.LandingPage;
import de.ii.ldproxy.ogcapi.common.domain.OgcApiExtent;
import de.ii.ldproxy.ogcapi.domain.ExternalDocumentation;
import de.ii.ldproxy.ogcapi.domain.PageRepresentation;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Value.Immutable
@JsonDeserialize(builder = de.ii.ldproxy.ogcapi.collections.domain.ImmutableCollections.Builder.class)
public abstract class Collections extends PageRepresentation {

    // restrict to information in ogcapi-stable, everything else goes into the extensions map

    public abstract List<String> getCrs();

    public abstract List<OgcApiCollection> getCollections();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<Collections> FUNNEL = (from, into) -> {
        PageRepresentation.FUNNEL.funnel(from, into);
        from.getCrs()
            .stream()
            .sorted()
            .forEachOrdered(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getCollections()
            .stream()
            .sorted(Comparator.comparing(OgcApiCollection::getId))
            .forEachOrdered(val -> OgcApiCollection.FUNNEL.funnel(val, into));
        from.getExtensions()
            .keySet()
            .stream()
            .sorted()
            .forEachOrdered(key -> into.putString(key, StandardCharsets.UTF_8));
        // we cannot encode the generic extension object
    };
}
