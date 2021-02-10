/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.Mergeable;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.Buildable;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableBuilder;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value.Immutable
@JsonDeserialize(builder = ImmutablePropertyTransformation.Builder.class)
public interface PropertyTransformation extends Buildable<PropertyTransformation>, Mergeable<PropertyTransformation> {

    abstract class Builder implements BuildableBuilder<PropertyTransformation> {
    }

    @Override
    default Builder getBuilder() {
        return new ImmutablePropertyTransformation.Builder().from(this);
    }

    Optional<String> getRename();

    Optional<String> getRemove();

    Optional<String> getStringFormat();

    Optional<String> getDateFormat();

    Optional<String> getCodelist();

    @Deprecated
    @JsonProperty(value = "null", access = JsonProperty.Access.WRITE_ONLY)
    Optional<String> getNull();

    @Value.Default
    default List<String> getNullify() {
        return getNull().map(ImmutableList::of)
                        .orElse(ImmutableList.of());
    }

    @Override
    default PropertyTransformation mergeInto(PropertyTransformation source) {
        return new ImmutablePropertyTransformation.Builder().from(source)
                                                            .from(this)
                                                            .nullify(Stream.concat(source.getNullify()
                                                                                         .stream(), getNullify().stream())
                                                                           .distinct()
                                                                           .collect(Collectors.toList()))
                                                            .build();
    }
}
