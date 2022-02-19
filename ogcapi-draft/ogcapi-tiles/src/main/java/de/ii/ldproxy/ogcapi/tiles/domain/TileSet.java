/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import de.ii.ldproxy.ogcapi.foundation.domain.Metadata2;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetData;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimits;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TilesBoundingBox;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileSet.Builder.class)
public abstract class TileSet extends Metadata2 {

    public enum DataType { map, vector, coverage }

    public abstract DataType getDataType();

    public abstract String getTileMatrixSetId();
    public abstract Optional<TileMatrixSetData> getTileMatrixSet();
    public abstract Optional<String> getTileMatrixSetURI();
    public abstract Optional<String> getTileMatrixSetDefinition();

    public abstract List<TileMatrixSetLimits> getTileMatrixSetLimits();
    public abstract TilesBoundingBox getBoundingBox();
    public abstract Optional<TilePoint> getCenterPoint();

    public abstract List<TileLayer> getLayers();

    // this is for map tiles, so we do not support this for now
    // public abstract Optional<StyleEntry> getStyle();

    // this is for offline tilesets, so we do not support this for now
    // public abstract Optional<MediaType> getMediaType();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();

    @SuppressWarnings("UnstableApiUsage")
    public static final Funnel<TileSet> FUNNEL = (from, into) -> {
        Metadata2.FUNNEL.funnel(from, into);
        into.putString(from.getDataType().toString(), StandardCharsets.UTF_8);
        into.putString(from.getTileMatrixSetId(), StandardCharsets.UTF_8);
        from.getTileMatrixSet().ifPresent(val -> TileMatrixSetData.FUNNEL.funnel(val, into));
        from.getTileMatrixSetURI().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getTileMatrixSetDefinition().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        TilesBoundingBox.FUNNEL.funnel(from.getBoundingBox(), into);
        from.getCenterPoint().ifPresent(val -> TilePoint.FUNNEL.funnel(val, into));
        from.getTileMatrixSetLimits()
            .stream()
            .sorted(Comparator.comparing(TileMatrixSetLimits::getTileMatrix))
            .forEachOrdered(val -> TileMatrixSetLimits.FUNNEL.funnel(val, into));
        from.getLayers()
            .stream()
            .sorted(Comparator.comparing(TileLayer::getId))
            .forEachOrdered(val -> TileLayer.FUNNEL.funnel(val, into));
        from.getExtensions()
            .keySet()
            .stream()
            .sorted()
            .forEachOrdered(key -> into.putString(key, StandardCharsets.UTF_8));
        // we cannot encode the generic extension object
    };
}
