/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.Wfs3Link;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(as = ImmutableStyleMetadata.class)
public interface StyleMetadata {

    Optional<String> getId();

    Optional<String> getTitle();

    Optional<String> getDescription();

    Optional<List<String>> getKeywords();

    Optional<String> getPointOfContact();

    Optional<String> getAccessConstraints();

    Optional<StyleDates> getDates();

    Optional<String> getScope();

    Optional<String> getVersion();

    Optional<List<StyleSheet>> getStylesheets();

    Optional<List<StyleLayer>> getLayers();

    Optional<List<Wfs3Link>> getLinks();
}
