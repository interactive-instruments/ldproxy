/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
public abstract class JsonSchemaArray extends JsonSchema {

  public final String getType() {
    return "array";
  }

  public abstract JsonSchema getItems();

  public abstract Optional<Integer> getMinItems();

  public abstract Optional<Integer> getMaxItems();

  public abstract static class Builder extends JsonSchema.Builder {}

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<JsonSchemaArray> FUNNEL =
      (from, into) -> {
        into.putString(from.getType(), StandardCharsets.UTF_8);
        JsonSchema.FUNNEL.funnel(from.getItems(), into);
        from.getMinItems().ifPresent(val -> into.putInt(val));
        from.getMaxItems().ifPresent(val -> into.putInt(val));
      };
}
