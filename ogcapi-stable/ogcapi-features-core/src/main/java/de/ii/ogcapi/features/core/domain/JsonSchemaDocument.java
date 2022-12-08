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
import java.util.Map;
import org.immutables.value.Value;

@SuppressWarnings("PMD.TooManyMethods")
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class JsonSchemaDocument extends JsonSchemaAbstractDocument {

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<JsonSchemaDocument> FUNNEL = JsonSchemaAbstractDocument.FUNNEL::funnel;

  @Override
  @JsonProperty("$schema")
  @Value.Derived
  public String getSchema() {
    return VERSION.V201909.url();
  }

  @Override
  @JsonProperty("$defs")
  public abstract Map<String, JsonSchema> getDefinitions();

  public abstract static class Builder extends JsonSchemaAbstractDocument.Builder {}
}
