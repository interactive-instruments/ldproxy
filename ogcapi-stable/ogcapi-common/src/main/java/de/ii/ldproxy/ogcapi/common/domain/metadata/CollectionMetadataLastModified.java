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
    default CollectionMetadataEntry updateWith(CollectionMetadataEntry delta) {
        if (Objects.isNull(delta))
            return this;
        if (!(delta instanceof CollectionMetadataLastModified))
            throw new IllegalStateException(String.format("Instance of CollectionMetadataEntry has invalid value. Expected 'CollectionMetadataLastModified', found '%s'", delta.getClass().getSimpleName()));
;
        return CollectionMetadataLastModified.of(union(this.getValue(), ((CollectionMetadataLastModified) delta).getValue()));
    }

    private static Instant union(@Nonnull Instant instant1, @Nonnull Instant instant2) {
        return instant1.isAfter(instant2) ? instant1 : instant2;
    }
}
