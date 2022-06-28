/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableGltfAsset.Builder.class)
public interface GltfAsset {

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
