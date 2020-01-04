/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTileMatrixSetLimits.Builder.class)
public abstract class TileMatrixSetLimits {
    public abstract String getTileMatrix();
    public abstract Integer getMinTileRow();
    public abstract Integer getMaxTileRow();
    public abstract Integer getMinTileCol();
    public abstract Integer getMaxTileCol();
}
