/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.infra.json.SchemaGenerator;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSetData;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class TileSetsFormatJson implements TileSetsFormatExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(MediaType.APPLICATION_JSON_TYPE)
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schemaTiles;
    public final static String SCHEMA_REF_TILES = "#/components/schemas/TileSets";

    public TileSetsFormatJson() {
        schemaTiles = schemaGenerator.getSchema(TileMatrixSetData.class);
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        if (path.endsWith("/tiles"))
            return new ImmutableOgcApiMediaTypeContent.Builder()
                    .schema(schemaTiles)
                    .schemaRef(SCHEMA_REF_TILES)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new RuntimeException("Unexpected path: " + path);
    }

    @Override
    public Object getTileSetsEntity(TileSets tiles, Optional<String> collectionId, OgcApiApi api, OgcApiRequestContext requestContext) {
        return tiles;
    }

}
