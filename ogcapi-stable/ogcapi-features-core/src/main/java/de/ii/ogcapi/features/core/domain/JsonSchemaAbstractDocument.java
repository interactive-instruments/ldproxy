/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("PMD.TooManyMethods")
public abstract class JsonSchemaAbstractDocument extends JsonSchemaAbstractObject {

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<JsonSchemaAbstractDocument> FUNNEL =
      (from, into) -> {
        JsonSchemaAbstractObject.FUNNEL.funnel(from, into);
        into.putString(from.getType(), StandardCharsets.UTF_8);
        from.getRequired().stream()
            .sorted()
            .forEachOrdered(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getProperties().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(entry -> JsonSchema.FUNNEL.funnel(entry.getValue(), into));
        from.getPatternProperties().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(entry -> JsonSchema.FUNNEL.funnel(entry.getValue(), into));
        into.putString(from.getSchema(), StandardCharsets.UTF_8);
        from.getId().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getDefinitions().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(entry -> JsonSchema.FUNNEL.funnel(entry.getValue(), into));
      };

  public abstract String getSchema();

  public abstract Map<String, JsonSchema> getDefinitions();

  @JsonProperty("$id")
  public abstract Optional<String> getId();

  public enum VERSION {
    V201909("https://json-schema.org/draft/2019-09/schema", "$defs"),
    V7("http://json-schema.org/draft-07/schema#", "definitions");

    private final String url;
    private final String defs;

    public static VERSION current() {
      return V201909;
    }

    VERSION(String url, String defs) {
      this.url = url;
      this.defs = defs;
    }

    public String url() {
      return url;
    }

    public String ref(String name) {
      return String.format("#/%s/%s", defs, name);
    }
  }

  public abstract static class Builder {
    public abstract Builder id(Optional<String> id);

    public abstract Builder definitions(Map<String, ? extends JsonSchema> entries);

    public abstract Builder putDefinitions(String key, JsonSchema value);

    public abstract Builder title(String title);

    public abstract Builder description(String description);

    public abstract Builder name(String name);

    @SuppressWarnings("PMD.LinguisticNaming")
    public abstract Builder isRequired(boolean isRequired);

    public abstract Builder required(Iterable<String> elements);

    public abstract Builder addRequired(String... elements);

    public abstract Builder properties(Map<String, ? extends JsonSchema> entries);

    public abstract Builder putProperties(String key, JsonSchema value);

    public abstract Builder patternProperties(Map<String, ? extends JsonSchema> entries);

    public abstract Builder additionalProperties(JsonSchema value);

    public abstract JsonSchemaAbstractDocument build();
  }
}
