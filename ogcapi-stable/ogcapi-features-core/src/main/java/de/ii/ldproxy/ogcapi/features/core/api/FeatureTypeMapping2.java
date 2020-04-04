/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableFeatureTypeMapping2.Builder.class)
public interface FeatureTypeMapping2 {

    Optional<String> getRename();

    Optional<String> getRemove();

    Optional<String> getStringFormat();

    Optional<String> getDateFormat();

    Optional<String> getCodelist();

    Optional<String> getNull();
}
