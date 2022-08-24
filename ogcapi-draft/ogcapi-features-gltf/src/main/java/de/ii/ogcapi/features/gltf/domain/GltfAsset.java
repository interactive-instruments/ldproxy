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
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableGltfAsset.Builder.class)
public interface GltfAsset {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<GltfAsset> FUNNEL =
      (from, into) -> {
        AssetMetadata.FUNNEL.funnel(from.getAsset(), into);
        from.getScene().ifPresent(into::putInt);
        from.getScenes().forEach(v -> Scene.FUNNEL.funnel(v, into));
        from.getNodes().forEach(v -> Node.FUNNEL.funnel(v, into));
        from.getMeshes().forEach(v -> Mesh.FUNNEL.funnel(v, into));
        from.getAccessors().forEach(v -> Accessor.FUNNEL.funnel(v, into));
        from.getBuffers().forEach(v -> Buffer.FUNNEL.funnel(v, into));
        from.getBufferViews().forEach(v -> BufferView.FUNNEL.funnel(v, into));
        from.getMaterials().forEach(v -> Material.FUNNEL.funnel(v, into));
        from.getExtensions().forEach((key, value) -> into.putString(key, StandardCharsets.UTF_8));
        from.getExtras().forEach((key, value) -> into.putString(key, StandardCharsets.UTF_8));
        from.getExtensionsRequired().forEach(v -> into.putString(v, StandardCharsets.UTF_8));
        from.getExtensionsUsed().forEach(v -> into.putString(v, StandardCharsets.UTF_8));
      };

  AssetMetadata getAsset();

  Optional<Integer> getScene();

  List<Scene> getScenes();

  List<Node> getNodes();

  List<Mesh> getMeshes();

  List<Accessor> getAccessors();

  List<Buffer> getBuffers();

  List<BufferView> getBufferViews();

  List<Material> getMaterials();

  Map<String, Object> getExtensions();

  Map<String, Object> getExtras();

  List<String> getExtensionsUsed();

  List<String> getExtensionsRequired();
}
