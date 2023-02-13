/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableAssetMetadata.Builder.class)
public interface AssetMetadata {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<AssetMetadata> FUNNEL =
      (from, into) -> {
        into.putString(from.getVersion(), StandardCharsets.UTF_8);
        into.putString(from.getGenerator(), StandardCharsets.UTF_8);
        from.getCopyright().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
      };

  @Value.Default
  default String getVersion() {
    return "2.0";
  }

  @Value.Default
  default String getGenerator() {
    return "ldproxy";
  }

  Optional<String> getCopyright();
}
