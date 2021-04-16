/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.Link;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(as = ImmutableStyleSheet.class)
public abstract class StyleSheet {

    public abstract Optional<String> getTitle();

    public abstract Optional<String> getVersion();

    public abstract  Optional<String> getSpecification();

    @JsonProperty("native")
    public abstract Optional<Boolean> native_();

    public abstract Optional<String> getTileMatrixSet();

    public abstract Optional<Link> getLink();
}
