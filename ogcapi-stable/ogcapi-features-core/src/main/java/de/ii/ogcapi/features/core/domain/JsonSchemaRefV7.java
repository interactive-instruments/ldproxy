/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class JsonSchemaRefV7 extends JsonSchemaRef {

  @JsonIgnore
  @Value.Auxiliary
  public String getDefsName() {
    return "definitions";
  }

  public abstract static class Builder extends JsonSchema.Builder {
  }
}
