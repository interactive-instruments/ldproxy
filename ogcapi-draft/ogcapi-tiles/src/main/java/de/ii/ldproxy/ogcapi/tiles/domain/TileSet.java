/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.Metadata2;
import de.ii.ldproxy.ogcapi.styles.domain.StyleEntry;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetData;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimits;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TilesBoundingBox;
import org.immutables.value.Value;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileSet.Builder.class)
public abstract class TileSet extends Metadata2 {

    public enum DataType { map, vector, coverage }

    @Override
    @JsonProperty("abstract")
    public abstract Optional<String> getDescription();

    public abstract Optional<MediaType> getMediaType();
    public abstract DataType getDataType();

    public abstract String getTileMatrixSetId();
    public abstract Optional<TileMatrixSetData> getTileMatrixSet();
    public abstract Optional<String> getTileMatrixSetURI();
    public abstract Optional<String> getTileMatrixSetDefinition();

    public abstract List<TileMatrixSetLimits> getTileMatrixSetLimits();

    public abstract Optional<TilesBoundingBox> getBoundingBox();

    /*
    public abstract List<TileLayer> getLayers();
    public abstract Optional<StyleEntry> getStyle();
     */
    public abstract Optional<TilePoint> getCenterPoint();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();
}
