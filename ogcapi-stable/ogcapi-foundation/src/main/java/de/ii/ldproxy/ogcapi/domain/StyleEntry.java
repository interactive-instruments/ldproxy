/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableStyleEntry.Builder.class)
public abstract class StyleEntry extends PageRepresentationWithId {

    @Value.Derived
    public List<Link> getLinksSorted() {
        return getLinks().stream()
                         .sorted(Comparator.comparing(Link::getTitle))
                         .collect(Collectors.toList());
    }
}
