/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.google.common.hash.Funnel;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
public abstract class JsonSchemaObject extends JsonSchemaAbstractObject {

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<JsonSchemaObject> FUNNEL = JsonSchemaAbstractObject.FUNNEL::funnel;

  public abstract static class Builder extends JsonSchema.Builder {}
}
