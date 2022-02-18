/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.maps.app;

import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.maps.domain.MapTileFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSet;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
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
