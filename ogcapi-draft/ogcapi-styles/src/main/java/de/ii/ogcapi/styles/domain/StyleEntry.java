/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.PageRepresentationWithId;
import org.immutables.value.Value;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableStyleEntry.Builder.class)
public abstract class StyleEntry extends PageRepresentationWithId {

    @JsonIgnore
    @Value.Lazy
    public List<Link> getLinksSorted() {
        return getLinks().stream()
                         .sorted(Comparator.comparing(Link::getTitle))
                         .collect(Collectors.toList());
    }

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<StyleEntry> FUNNEL = PageRepresentationWithId.FUNNEL::funnel;
}
