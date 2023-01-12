/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.google.common.hash.Funnel;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableJsonSchemaTrue.Builder.class)
@JsonSerialize(converter = JsonSchemaTrue.ToStringConverter.class)
public abstract class JsonSchemaTrue extends JsonSchema {

  // any instance is valid, same as {}

  @JsonIgnore
  @Value.Derived
  public String getType() {
    return "true";
  }

  public static class ToStringConverter extends StdConverter<JsonSchemaTrue, String> {

    @Override
    public String convert(JsonSchemaTrue value) {
      return value.getType();
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<JsonSchemaTrue> FUNNEL = (from, into) -> {};
}
