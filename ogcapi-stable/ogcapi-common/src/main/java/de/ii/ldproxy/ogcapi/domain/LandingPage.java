/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

@Value.Immutable
@JsonDeserialize(builder = ImmutableLandingPage.Builder.class)
public abstract class LandingPage extends PageRepresentation {

    public abstract Optional<OgcApiExtent> getExtent();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();
}
