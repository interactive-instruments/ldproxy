/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.PageRepresentation;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileCollection.Builder.class)
public abstract class TileCollection extends PageRepresentation {

    public abstract Optional<String> getTileMatrixSet();
    public abstract Optional<String> getTileMatrixSetURI();
    public abstract List<TileMatrixSetLimits> getTileMatrixSetLimits();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableTileMatrixSetLimits.Builder.class)
    public static abstract class TileMatrixSetLimits {
        public abstract String getTileMatrix();
        public abstract Integer getMinTileRow();
        public abstract Integer getMaxTileRow();
        public abstract Integer getMinTileCol();
        public abstract Integer getMaxTileCol();
    }
}
