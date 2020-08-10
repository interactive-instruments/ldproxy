/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.tileMatrixSet;

import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.infra.json.SchemaGenerator;
import de.ii.ldproxy.ogcapi.tiles.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSetData;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSets;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSetsFormatExtension;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Provides
@Instantiate
public class TileMatrixSetsFormatJson implements TileMatrixSetsFormatExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
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
    public String getPathPattern() {
        return "^/tileMatrixSets(?:/\\w+)?/?$";
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        if (path.equals("/tileMatrixSets"))
            return new ImmutableOgcApiMediaTypeContent.Builder()
                    .schema(schemaStyleTileMatrixSets)
                    .schemaRef(SCHEMA_REF_TILE_MATRIX_SETS)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();
        else if (path.equals("/tileMatrixSets/{tileMatrixSetId}"))
            return new ImmutableOgcApiMediaTypeContent.Builder()
                    .schema(schemaStyleTileMatrixSet)
                    .schemaRef(SCHEMA_REF_TILE_MATRIX_SET)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new ServerErrorException("Unexpected path "+path,500);
    }

    @Override
    public Object getTileMatrixSetsEntity(TileMatrixSets tileMatrixSets, OgcApiApi api, OgcApiRequestContext requestContext) {
        return tileMatrixSets;
    }

    @Override
    public Object getTileMatrixSetEntity(TileMatrixSetData tileMatrixSet, OgcApiApi api, OgcApiRequestContext requestContext) {
        return tileMatrixSet;
    }

}
