/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain.metadata;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableCollectionMetadataLastModified.Builder.class)
public interface CollectionMetadataLastModified extends CollectionMetadataEntry {

    static CollectionMetadataLastModified of(Instant lastModified) {
        return new ImmutableCollectionMetadataLastModified.Builder().value(Objects.requireNonNullElse(lastModified, Instant.MIN))
                                                                    .build();
    }

    @Override
    Instant getValue();

    @Override
    default Optional<CollectionMetadataEntry> updateWith(CollectionMetadataEntry delta) {
        if (Objects.isNull(delta) || Objects.isNull(delta.getValue()))
            return Optional.empty();
        if (Objects.isNull(getValue()))
            return Optional.of(delta);
        if (!(delta instanceof CollectionMetadataLastModified))
            throw new IllegalStateException(String.format("Instance of CollectionMetadataEntry has invalid value. Expected 'CollectionMetadataLastModified', found '%s'", delta.getClass().getSimpleName()));

        Instant deltaInstant = ((CollectionMetadataLastModified) delta).getValue();
        if (this.getValue().isAfter(deltaInstant))
            return Optional.empty();

        return Optional.of(delta);
    }
}
