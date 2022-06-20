/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.google.common.hash.Funnel;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class OgcResourceMetadata {

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<OgcResourceMetadata> FUNNEL = (from, into) -> {
        from.getTitle().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getDescription().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getLinks()
                .stream()
                .sorted(Comparator.comparing(Link::getHref))
                .forEachOrdered(link -> into.putString(link.getHref(), StandardCharsets.UTF_8)
                        .putString(Objects.requireNonNullElse(link.getRel(), ""), StandardCharsets.UTF_8));
        from.getKeywords()
                .stream()
                .sorted()
                .forEachOrdered(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getPublisher().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getPointOfContact().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getLicense().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getAttribution().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getDates().ifPresent(val -> MetadataDates.FUNNEL.funnel(val, into));
        from.getVersion().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
    };

    public abstract Optional<String> getTitle();

    public abstract Optional<String> getDescription();

    public abstract List<Link> getLinks();

    public abstract List<String> getKeywords();

    public abstract Optional<String> getPublisher();

    public abstract Optional<String> getPointOfContact();

    public abstract Optional<String> getAccessConstraints();

    public abstract Optional<String> getLicense();

    public abstract Optional<String> getAttribution();

    public abstract Optional<MetadataDates> getDates();

    public abstract Optional<String> getVersion();
}
