/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.immutables.value.Value;

public abstract class JsonSchemaRefInternal extends JsonSchema {

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<JsonSchemaRefInternal> FUNNEL =
      (from, into) -> {
        into.putString(from.getRef(), StandardCharsets.UTF_8);
      };

  @JsonProperty("$ref")
  @Value.Derived
  public String getRef() {
    return String.format("#/%s/%s", getDefsName(), getObjectType());
  }

  @JsonIgnore
  @Value.Auxiliary
  public abstract String getDefsName();

  @JsonIgnore
  @Value.Auxiliary
  public abstract String getObjectType();

  @JsonIgnore
  @Nullable
  @Value.Auxiliary
  public abstract JsonSchema getDef();
}
