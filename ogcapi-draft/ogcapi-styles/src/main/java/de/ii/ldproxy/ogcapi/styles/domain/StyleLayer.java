/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.Link;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableStyleLayer.class)
public abstract class StyleLayer {

    public abstract String getId();

    public abstract Optional<String> getDescription();

    public abstract Optional<String> getType();

    public abstract Map<String, Object> getAttributes();

    public abstract Optional<Link> getSampleData();

    @JsonIgnore
    @Value.Lazy
    public Optional<String> getAttributeList() {
        return getAttributes().isEmpty() ? Optional.empty() : Optional.of(String.join(", ", getAttributes().keySet().stream().sorted().collect(Collectors.toUnmodifiableList())));
    }
}
