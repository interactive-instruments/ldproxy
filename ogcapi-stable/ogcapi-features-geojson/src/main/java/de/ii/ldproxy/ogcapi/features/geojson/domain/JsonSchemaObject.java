/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableJsonSchemaObject.class)
public abstract class JsonSchemaObject extends JsonSchema {

    public final String getType() { return "object"; }

    public abstract List<String> getRequired();
    public abstract Map<String, JsonSchema> getProperties();
    public abstract Map<String, JsonSchema> getPatternProperties();

    // Only use the following in JSON Schema documents, not in embedded schemas
    @JsonProperty("$schema")
    public abstract Optional<String> getSchema();
    @JsonProperty("$id")
    public abstract Optional<String> getId();
    @JsonProperty("$defs")
    public abstract Optional<Map<String, JsonSchema>> getDefs();
    @JsonProperty("definitions")
    public abstract Optional<Map<String, JsonSchema>> getDefinitions();

}
