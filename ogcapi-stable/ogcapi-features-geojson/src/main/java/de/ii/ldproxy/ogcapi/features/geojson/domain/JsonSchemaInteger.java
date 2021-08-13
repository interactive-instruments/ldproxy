/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaInteger;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableJsonSchemaInteger.class)
public abstract class JsonSchemaInteger extends JsonSchema {

    public final String getType() { return "integer"; }

    public abstract Optional<Long> getMinimum();
    public abstract Optional<Long> getMaximum();
    @JsonProperty("enum")
    public abstract List<Integer> getEnums();
}
