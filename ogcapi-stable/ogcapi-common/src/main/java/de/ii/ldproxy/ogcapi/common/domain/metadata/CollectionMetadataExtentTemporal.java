/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain.metadata;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ImmutableTemporalExtent;
import de.ii.ldproxy.ogcapi.domain.TemporalExtent;
import org.immutables.value.Value;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableCollectionMetadataExtentTemporal.Builder.class)
public interface CollectionMetadataExtentTemporal extends CollectionMetadataEntry {

    static CollectionMetadataExtentTemporal of(Long start, Long end) {
        return of(TemporalExtent.of(Objects.requireNonNullElse(start, Long.MIN_VALUE),
                                    Objects.requireNonNullElse(end, Long.MAX_VALUE)));
    }

    static CollectionMetadataExtentTemporal of(TemporalExtent interval) {
        return new ImmutableCollectionMetadataExtentTemporal.Builder().value(Objects.requireNonNullElse(interval, TemporalExtent.of(Long.MIN_VALUE, Long.MAX_VALUE)))
                                                                      .build();
    }

    @Override
    TemporalExtent getValue();

    @Override
    default Optional<CollectionMetadataEntry> updateWith(CollectionMetadataEntry delta) {
        if (Objects.isNull(delta) || Objects.isNull(delta.getValue()))
            return Optional.empty();
        if (Objects.isNull(getValue()))
            return Optional.of(delta);
        if (!(delta instanceof CollectionMetadataExtentTemporal))
            throw new IllegalStateException(String.format("Instance of CollectionMetadataEntry has invalid value. Expected 'CollectionMetadataExtentTemporal', found '%s'", delta.getClass().getSimpleName()));

        TemporalExtent deltaExtent = ((CollectionMetadataExtentTemporal) delta).getValue();
        long currentStart = Objects.requireNonNullElse(getValue().getStart(), Long.MIN_VALUE);
        long currentEnd = Objects.requireNonNullElse(getValue().getEnd(), Long.MAX_VALUE);
        long deltaStart = Objects.requireNonNullElse(getValue().getStart(), Long.MIN_VALUE);
        long deltaEnd = Objects.requireNonNullElse(getValue().getEnd(), Long.MAX_VALUE);

        if (currentStart <= deltaStart && currentEnd >= deltaEnd)
            return Optional.empty();

        return Optional.of(CollectionMetadataExtentTemporal.of(TemporalExtent.of(Math.min(currentStart, deltaStart),
                                                                                 Math.max(currentEnd, deltaEnd))));
    }
}
