/**
 * Copyright 2021 interactive instruments GmbH
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
import java.util.Objects;

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
    default CollectionMetadataEntry updateWith(CollectionMetadataEntry delta) {
        if (Objects.isNull(delta))
            return this;
        if (!(delta instanceof CollectionMetadataExtentTemporal))
            throw new IllegalStateException(String.format("Instance of CollectionMetadataEntry has invalid value. Expected 'CollectionMetadataExtentTemporal', found '%s'", delta.getClass().getSimpleName()));
;
        return CollectionMetadataExtentTemporal.of(union(this.getValue(), (TemporalExtent) delta.getValue()));
    }

    private static TemporalExtent union(@Nonnull TemporalExtent int1, @Nonnull TemporalExtent int2) {
        return new ImmutableTemporalExtent.Builder().start(Objects.isNull(int1.getStart()) ? null : Objects.isNull(int2.getStart()) ? null : Math.min(int1.getStart(), int2.getStart()))
                                                    .end(Objects.isNull(int1.getEnd()) ? null : Objects.isNull(int2.getEnd()) ? null : Math.max(int1.getEnd(), int2.getEnd()))
                                                    .build();
    }
}
