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
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
public abstract class JsonSchemaString extends JsonSchema {

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<JsonSchemaString> FUNNEL =
      (from, into) -> {
        JsonSchema.FUNNEL.funnel(from, into);
        into.putString(from.getType(), StandardCharsets.UTF_8);
        from.getFormat().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getPattern().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getEnums().stream()
            .sorted()
            .forEachOrdered(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getUnit().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
      };

  public final String getType() {
    return "string";
  }

  public abstract Optional<String> getFormat();

  public abstract Optional<String> getPattern();

  @JsonProperty("enum")
  public abstract List<String> getEnums();

  public abstract Optional<String> getUnit();

  public abstract static class Builder extends JsonSchema.Builder {}
}
