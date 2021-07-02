/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.Metadata2;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaObject;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TilesBoundingBox;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableTileLayer.class)
public abstract class TileLayer extends Metadata2 {

    public enum GeometryType { points, lines, polygons }

    public abstract String getId();

    public abstract TileSet.DataType getDataType();
    public abstract Optional<String> getFeatureType();
    public abstract Optional<GeometryType> getGeometryType();

    public abstract Optional<String> getTheme();

    public abstract Optional<String> getMinTileMatrix();
    public abstract Optional<String> getMaxTileMatrix();
    public abstract Optional<Double> getMinCellSize();
    public abstract Optional<Double> getMaxCellSize();
    public abstract Optional<Double> getMinScaleDenominator();
    public abstract Optional<Double> getMaxScaleDenominator();

    public abstract Optional<TilesBoundingBox> getBoundingBox();

    public abstract Optional<JsonSchemaObject> getPropertiesSchema();

    // this is for map tiles, so we do not support the following for now:
    // public abstract Optional<StyleEntry> getStyle();

}
