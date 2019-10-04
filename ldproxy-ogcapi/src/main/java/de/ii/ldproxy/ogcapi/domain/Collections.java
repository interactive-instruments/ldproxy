/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutableCollections.Builder.class)
public abstract class Collections extends PageRepresentation {

    public abstract List<String> getCrs();

    public abstract List<OgcApiCollection> getCollections();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();
}
