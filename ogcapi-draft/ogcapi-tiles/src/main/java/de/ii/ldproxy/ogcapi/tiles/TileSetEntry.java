/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;


@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = ImmutableTileSetEntry.Builder.class)
public interface TileSetEntry {

    String getTileURL();
    int getTileMatrix();
    int getTileRow();
    int getTileCol();
    @Nullable Integer getWidth();
    @Nullable Integer getHeight();
    @Nullable Integer getTop();
    @Nullable Integer getLeft();

}
