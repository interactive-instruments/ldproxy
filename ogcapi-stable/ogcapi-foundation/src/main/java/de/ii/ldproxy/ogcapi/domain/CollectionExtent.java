/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonDeserialize(builder = ImmutableCollectionExtent.Builder.class)
public interface CollectionExtent {

    Optional<TemporalExtent> getTemporal();

    Optional<BoundingBox> getSpatial();

    Optional<Boolean> getSpatialComputed();

    Optional<Boolean> getTemporalComputed();
}
