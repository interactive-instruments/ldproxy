/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.maps.app;

import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.maps.domain.MapTileFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSet;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.core.MediaType;

@Component
@Provides
@Instantiate
public class MapTileFormatJPEG extends MapTileFormatExtension {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("image","jpeg"))
            .label("JPEG")
            .parameter("jpeg")
            .build();

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        if (path.equals("/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}") ||
            path.equals("/collections/{collectionId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"))
            return new ImmutableApiMediaTypeContent.Builder()
                    .schema(SCHEMA_TILE)
                    .schemaRef(SCHEMA_REF_TILE)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        return null;
    }

    @Override
    public String getExtension() {
        return "jpeg";
    }
}
