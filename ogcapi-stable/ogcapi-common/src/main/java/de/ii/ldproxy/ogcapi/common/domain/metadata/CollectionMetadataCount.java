/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain.metadata;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableCollectionMetadataCount.Builder.class)
public interface CollectionMetadataCount extends CollectionMetadataEntry {

    static CollectionMetadataCount of(long itemCount) {
        return new ImmutableCollectionMetadataCount.Builder().value(Objects.requireNonNullElse(itemCount, 0L))
                                                             .build();
    }

    @Override
    Long getValue();

    @Override
    default Optional<CollectionMetadataEntry> updateWith(CollectionMetadataEntry delta) {
        if (Objects.isNull(delta) || Objects.isNull(delta.getValue()))
            return Optional.empty();
        if (Objects.isNull(getValue()))
            return Optional.of(delta);
        if (!(delta instanceof CollectionMetadataCount))
            throw new IllegalStateException(String.format("Instance of CollectionMetadataEntry has invalid value. Expected 'CollectionMetadataCount', found '%s'", delta.getClass().getSimpleName()));

        Long deltaCount = ((CollectionMetadataCount) delta).getValue();
        if (deltaCount == 0)
            return Optional.of(delta);

        return Optional.of(CollectionMetadataCount.of(this.getValue() + deltaCount));
    }
}
