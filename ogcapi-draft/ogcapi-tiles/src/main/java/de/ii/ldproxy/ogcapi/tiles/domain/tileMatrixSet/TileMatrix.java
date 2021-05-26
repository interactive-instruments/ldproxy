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
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileMatrix.Builder.class)
public abstract class TileMatrix {

    public String getId() { return String.valueOf(getTileLevel()); }
    public abstract Optional<String> getTitle();
    public abstract Optional<String> getAbstract();
    public abstract List<String> getKeywords();
    public abstract long getTileWidth();
    public abstract long getTileHeight();
    public abstract long getMatrixWidth();
    public abstract long getMatrixHeight();
    public abstract double getScaleDenominator();
    public double getCellSize() {  return getScaleDenominator() * 0.00028 / getMetersPerUnit(); }
    public abstract double[] getPointOfOrigin();
    public String getCornerOfOrigin() { return "topLeft"; }

    @JsonIgnore
    public abstract double getMetersPerUnit();

    @JsonIgnore
    public abstract int getTileLevel();

}
