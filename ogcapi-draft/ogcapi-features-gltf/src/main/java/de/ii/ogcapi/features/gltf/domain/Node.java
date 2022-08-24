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
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableNode.Builder.class)
public interface Node {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<Node> FUNNEL =
      (from, into) -> {
        from.getName().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        from.getMesh().ifPresent(into::putInt);
        from.getChildren().forEach(into::putInt);
        from.getTranslation().forEach(into::putDouble);
        from.getRotation().forEach(into::putDouble);
        from.getScale().forEach(into::putDouble);
        from.getMatrix().forEach(into::putDouble);
      };

  Optional<String> getName();

  Optional<Integer> getMesh();

  List<Integer> getChildren();

  List<Double> getTranslation();

  List<Double> getRotation();

  List<Double> getScale();

  List<Double> getMatrix();
}
