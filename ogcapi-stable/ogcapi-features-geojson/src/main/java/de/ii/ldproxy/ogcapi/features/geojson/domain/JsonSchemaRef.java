/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaRef;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableJsonSchemaRef.class)
public abstract class JsonSchemaRef extends JsonSchema {

    @JsonIgnore
    public final String getType() { return "$ref"; }

    @JsonProperty("$ref")
    @Value.Derived
    public String getRef() {
        return String.format("#/%s/%s", getDefsName(), getObjectType());
    }

    @JsonIgnore
    @Value.Auxiliary
    public String getDefsName() {
        return "$defs";
    }

    @JsonIgnore
    @Value.Auxiliary
    public abstract String getObjectType();

    @JsonIgnore
    @Nullable
    @Value.Auxiliary
    public abstract JsonSchema getDef();
}
