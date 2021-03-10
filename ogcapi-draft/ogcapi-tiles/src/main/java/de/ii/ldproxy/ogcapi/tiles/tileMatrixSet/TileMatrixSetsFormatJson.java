/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.tileMatrixSet;

import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.SchemaGenerator;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;

@Component
@Provides
@Instantiate
public class TileMatrixSetsFormatJson implements TileMatrixSetsFormatExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.APPLICATION_JSON_TYPE)
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schemaStyleTileMatrixSets;
    public final static String SCHEMA_REF_TILE_MATRIX_SETS = "#/components/schemas/TileMatrixSets";
    private final Schema schemaStyleTileMatrixSet;
    public final static String SCHEMA_REF_TILE_MATRIX_SET = "#/components/schemas/TileMatrixSet";

    public TileMatrixSetsFormatJson() {
        schemaStyleTileMatrixSet = schemaGenerator.getSchema(TileMatrixSetData.class);
        schemaStyleTileMatrixSets = schemaGenerator.getSchema(TileMatrixSets.class);
    }

    @Requires
    private I18n i18n;

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        if (path.equals("/tileMatrixSets"))
            return new ImmutableApiMediaTypeContent.Builder()
                    .schema(schemaStyleTileMatrixSets)
                    .schemaRef(SCHEMA_REF_TILE_MATRIX_SETS)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();
        else if (path.equals("/tileMatrixSets/{tileMatrixSetId}"))
            return new ImmutableApiMediaTypeContent.Builder()
                    .schema(schemaStyleTileMatrixSet)
                    .schemaRef(SCHEMA_REF_TILE_MATRIX_SET)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new RuntimeException("Unexpected path: " + path);
    }

    @Override
    public Object getTileMatrixSetsEntity(TileMatrixSets tileMatrixSets, OgcApi api, ApiRequestContext requestContext) {
        return tileMatrixSets;
    }

    @Override
    public Object getTileMatrixSetEntity(TileMatrixSetData tileMatrixSet, OgcApi api, ApiRequestContext requestContext) {
        return tileMatrixSet;
    }

}
