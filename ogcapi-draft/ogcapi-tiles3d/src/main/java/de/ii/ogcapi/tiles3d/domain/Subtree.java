/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import de.ii.ogcapi.features.gltf.domain.Buffer;
import de.ii.ogcapi.features.gltf.domain.BufferView;
import de.ii.ogcapi.features.gltf.domain.PropertyTable;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableSubtree.Builder.class)
public interface Subtree {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<Subtree> FUNNEL =
      (from, into) -> {
        from.getBuffers().forEach(v -> Buffer.FUNNEL.funnel(v, into));
        from.getBufferViews().forEach(v -> BufferView.FUNNEL.funnel(v, into));
        from.getPropertyTables().forEach(v -> PropertyTable.FUNNEL.funnel(v, into));
        Availability.FUNNEL.funnel(from.getTileAvailability(), into);
        from.getContentAvailability().forEach(v -> Availability.FUNNEL.funnel(v, into));
        Availability.FUNNEL.funnel(from.getChildSubtreeAvailability(), into);
        from.getTileMetadata().ifPresent(into::putInt);
        from.getContentMetadata().forEach(into::putInt);
        from.getSubtreeMetadata().ifPresent(v -> MetadataEntity.FUNNEL.funnel(v, into));
      };

  List<Buffer> getBuffers();

  List<BufferView> getBufferViews();

  List<PropertyTable> getPropertyTables();

  Availability getTileAvailability();

  List<Availability> getContentAvailability();

  Availability getChildSubtreeAvailability();

  Optional<Integer> getTileMetadata();

  List<Integer> getContentMetadata();

  Optional<MetadataEntity> getSubtreeMetadata();
}
