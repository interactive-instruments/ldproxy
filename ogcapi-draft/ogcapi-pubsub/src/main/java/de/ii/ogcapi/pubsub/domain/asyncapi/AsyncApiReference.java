/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.domain.asyncapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface AsyncApiReference {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<AsyncApiReference> FUNNEL =
      (from, into) -> {
        from.getRef().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
      };

  @JsonProperty("$ref")
  Optional<String> getRef();
}
