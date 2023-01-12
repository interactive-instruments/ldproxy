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
@JsonDeserialize(builder = ImmutableJsonSchemaFalse.Builder.class)
@JsonSerialize(converter = JsonSchemaFalse.ToStringConverter.class)
public abstract class JsonSchemaFalse extends JsonSchema {

  // any instance is invalid

  @JsonIgnore
  @Value.Derived
  public String getType() {
    return "false";
  }

  public static class ToStringConverter extends StdConverter<JsonSchemaFalse, String> {

    @Override
    public String convert(JsonSchemaFalse value) {
      return value.getType();
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<JsonSchemaFalse> FUNNEL = (from, into) -> {};
}
