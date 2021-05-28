/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.Link;
import org.immutables.value.Value;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * This class specifies the data structure of a tile matrix set.
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileMatrixSetData.Builder.class)
public abstract class TileMatrixSetData {

    public abstract String getId();

    public abstract Optional<String> getTitle();

    public abstract Optional<String> getDescription();

    public abstract List<String> getKeywords();

    public abstract List<Link> getLinks();

    public abstract String getSupportedCRS();

    public abstract Optional<URI> getWellKnownScaleSet();

    public abstract Optional<TilesBoundingBox> getBoundingBox();

    public abstract List<TileMatrix> getTileMatrices();

    public abstract List<String> getOrderedAxes();
}
