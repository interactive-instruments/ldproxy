/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.Metadata2;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableStylesheetMetadata.class)
public abstract class StylesheetMetadata {

    public abstract Optional<String> getTitle();

    public abstract Optional<String> getVersion();

    public abstract  Optional<String> getSpecification();

    @JsonProperty("native")
    public abstract Optional<Boolean> native_();

    public abstract Optional<String> getTileMatrixSet();

    public abstract Optional<Link> getLink();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<StylesheetMetadata> FUNNEL = (from, into) -> {
        from.getTitle().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getVersion().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getSpecification().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.native_().ifPresent(into::putBoolean);
        from.getTileMatrixSet().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getLink().ifPresent(link -> into.putString(link.getHref(), StandardCharsets.UTF_8)
                                             .putString(Objects.requireNonNullElse(link.getRel(), ""), StandardCharsets.UTF_8));
    };

}
