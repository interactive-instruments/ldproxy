/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import de.ii.ldproxy.ogcapi.foundation.domain.Link;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableRoutes.Builder.class)
public abstract class Routes {
    public abstract List<Link> getLinks();

    @JsonIgnore
    public abstract Optional<RouteDefinitionInfo> getTemplateInfo();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<Routes> FUNNEL = (from, into) -> {
        from.getLinks()
            .stream()
            .sorted(Comparator.comparing(Link::getHref))
            .forEachOrdered(link -> into.putString(link.getHref(), StandardCharsets.UTF_8)
                .putString(Objects.requireNonNullElse(link.getRel(), ""), StandardCharsets.UTF_8));
        from.getTemplateInfo().ifPresent(val -> RouteDefinitionInfo.FUNNEL.funnel(val, into));
    };
}
