/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.domain.asyncapi;

import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface AsyncApiInfo {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<AsyncApiInfo> FUNNEL =
      (from, into) -> {
        into.putString(from.getTitle(), StandardCharsets.UTF_8);
        from.getDescription().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        into.putString(from.getVersion(), StandardCharsets.UTF_8);
        from.getContact().ifPresent(v -> AsyncApiContact.FUNNEL.funnel(v, into));
        from.getLicense().ifPresent(v -> AsyncApiLicense.FUNNEL.funnel(v, into));
      };

  String getTitle();

  Optional<String> getDescription();

  @Value.Default
  default String getVersion() {
    return "1.0.0";
  }

  Optional<AsyncApiContact> getContact();

  Optional<AsyncApiLicense> getLicense();
}
