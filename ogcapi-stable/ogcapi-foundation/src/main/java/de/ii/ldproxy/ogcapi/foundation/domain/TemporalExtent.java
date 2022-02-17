/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.foundation.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTemporalExtent.Builder.class)
public interface TemporalExtent {

    @Value.Default
    @Nullable
    default Long getStart() {
        return null;
    }

    @Value.Default
    @Nullable
    default Long getEnd() {
        return null;
    }

    default String humanReadable(Locale locale) {
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, locale);

        return String.format("%s - %s",
            Optional.ofNullable(getStart()).map(start -> df.format(new Date(start))).orElse(".."),
            Optional.ofNullable(getEnd()).map(end -> df.format(new Date(end))).orElse(".."));
    }
}
