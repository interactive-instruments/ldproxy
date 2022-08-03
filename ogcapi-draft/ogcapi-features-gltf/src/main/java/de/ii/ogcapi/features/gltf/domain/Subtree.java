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
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableSubtree.Builder.class)
public interface Subtree {

  List<Buffer> getBuffers();

  List<BufferView> getBufferViews();

  List<PropertyTable> getPropertyTables();

  Availability getTileAvailability();

  List<Availability> getContentAvailability();

  Availability getChildSubtreeAvailability();

  Optional<Integer> getTileMetadata();

  List<Integer> getContentMetadata();

  MetadataEntity getSubtreeMetadata();
}
