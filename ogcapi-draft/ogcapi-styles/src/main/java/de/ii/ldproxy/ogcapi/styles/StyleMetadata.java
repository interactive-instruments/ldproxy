/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.PageRepresentation;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(as = ImmutableStyleMetadata.class)
public abstract class StyleMetadata extends PageRepresentation {

    public abstract Optional<String> getId();

    public abstract Optional<List<String>> getKeywords();

    public abstract Optional<String> getPointOfContact();

    public abstract Optional<String> getAccessConstraints();

    public abstract Optional<StyleDates> getDates();

    public abstract Optional<String> getScope();

    public abstract Optional<String> getVersion();

    public abstract Optional<List<StyleSheet>> getStylesheets();

    public abstract Optional<List<StyleLayer>> getLayers();
}
