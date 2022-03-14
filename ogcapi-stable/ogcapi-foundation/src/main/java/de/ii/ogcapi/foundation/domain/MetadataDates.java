/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableMetadataDates.class)
public abstract class MetadataDates {

    public abstract Optional<String> getCreation();
    public abstract Optional<String> getPublication();
    public abstract Optional<String> getRevision();
    public abstract Optional<String> getValidTill();
    public abstract Optional<String> getReceivedOn();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<MetadataDates> FUNNEL = (from, into) -> {
        from.getCreation().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getPublication().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getRevision().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getValidTill().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getReceivedOn().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
    };

}
