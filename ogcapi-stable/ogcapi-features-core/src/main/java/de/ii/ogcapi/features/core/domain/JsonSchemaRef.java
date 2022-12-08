/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.hash.Funnel;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
public abstract class JsonSchemaRef extends JsonSchemaRefInternal {

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<JsonSchemaRef> FUNNEL = JsonSchemaRefInternal.FUNNEL::funnel;

  @Override
  @JsonIgnore
  @Value.Auxiliary
  public String getDefsName() {
    return "$defs";
  }

  public abstract static class Builder extends JsonSchema.Builder {}
}
