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
public interface AsyncApiLicense {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<AsyncApiLicense> FUNNEL =
      (from, into) -> {
        into.putString(from.getName(), StandardCharsets.UTF_8);
        from.getUrl().ifPresent(s -> into.putString(s.toString(), StandardCharsets.UTF_8));
      };

  String getName();

  Optional<URI> getUrl();

  @Value.Check
  default void check() {
    Preconditions.checkState(
        getUrl().filter(val -> !val.isAbsolute()).isEmpty(),
        "AsyncAPI: The license URL must be absolute. Found: %s",
        getUrl());
  }
}
