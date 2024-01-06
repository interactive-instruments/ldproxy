/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.domain.asyncapi;

import com.google.common.base.Preconditions;
import com.google.common.hash.Funnel;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface AsyncApiContact {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<AsyncApiContact> FUNNEL =
      (from, into) -> {
        from.getName().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getUrl().ifPresent(s -> into.putString(s.toString(), StandardCharsets.UTF_8));
        from.getEmail().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
      };

  Optional<String> getName();

  Optional<URI> getUrl();

  Optional<String> getEmail();

  @Value.Check
  default void check() {
    Preconditions.checkState(
        getUrl().filter(val -> !val.isAbsolute()).isEmpty(),
        "AsyncAPI: The contact URL must be absolute. Found: %s",
        getUrl());
    Preconditions.checkState(
        getEmail().filter(val -> !val.matches("^(.+)@(\\S+)$")).isEmpty(),
        "AsyncAPI: The contact email address is invalid. Found: %s",
        getEmail());
  }
}
