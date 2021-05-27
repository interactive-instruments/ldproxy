/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTilesBoundingBox.Builder.class)
public abstract class TilesBoundingBox {


    public abstract double[] getLowerLeft();
    public abstract double[] getUpperRight();

    /**
     * the coordinate reference system that is the basis of this tiling scheme
     * @return
     */
    @JsonIgnore
    public abstract Optional<EpsgCrs> getCrsEpsg();

    @Value.Derived
    @Value.Auxiliary
    public Optional<String> getCrs() { return getCrsEpsg().map(EpsgCrs::toUriString); }
}
