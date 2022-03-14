/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.json;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.SchemaInfo;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.SchemaGenerator;
import de.ii.ogcapi.tiles.domain.TileSet;
import de.ii.ogcapi.tiles.domain.TileSetFormatExtension;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class TileSetFormatJson implements TileSetFormatExtension {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.APPLICATION_JSON_TYPE)
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schemaTileSetJson;
    private final FeaturesCoreProviders providers;
    private final SchemaInfo schemaInfo;
    public final static String SCHEMA_REF_TILE_SET_JSON = "#/components/schemas/TileSetJson";

    @Inject
    public TileSetFormatJson(SchemaGenerator schemaGenerator,
                             FeaturesCoreProviders providers,
                             SchemaInfo schemaInfo) {
        schemaTileSetJson = schemaGenerator.getSchema(TileSet.class);
        this.providers = providers;
        this.schemaInfo = schemaInfo;
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        if (path.endsWith("/tiles/{tileMatrixSetId}"))
            return new ImmutableApiMediaTypeContent.Builder()
                    .schema(schemaTileSetJson)
                    .schemaRef(SCHEMA_REF_TILE_SET_JSON)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new RuntimeException("Unexpected path: " + path);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    @Override
    public Object getTileSetEntity(TileSet tileset, OgcApiDataV2 apiData, Optional<String> collectionId, ApiRequestContext requestContext) {
        return tileset;
    }
}
