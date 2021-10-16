/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.google.common.hash.Funnel;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public abstract class Metadata2 extends PageRepresentation {

    public abstract List<String> getKeywords();

    public abstract Optional<String> getPublisher();

    public abstract Optional<String> getPointOfContact();

    public abstract Optional<String> getAccessConstraints();

    public abstract Optional<String> getLicense();

    public abstract Optional<String> getAttribution();

    public abstract Optional<MetadataDates> getDates();

    public abstract Optional<String> getVersion();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<Metadata2> FUNNEL = (from, into) -> {
        PageRepresentation.FUNNEL.funnel(from, into);
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

}
