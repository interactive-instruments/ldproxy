/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.json;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.SchemaInfo;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ClassSchemaCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles.app.TilesHelper;
import de.ii.ogcapi.tiles.domain.ImmutableTileJson;
import de.ii.ogcapi.tiles.domain.TileJson;
import de.ii.ogcapi.tiles.domain.TileSet;
import de.ii.ogcapi.tiles.domain.TileSetFormatExtension;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import io.swagger.v3.oas.models.media.Schema;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class TileSetFormatTileJson implements TileSetFormatExtension {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application","vnd.mapbox.tile+json"))
            .label("TileJSON")
            .parameter("tilejson")
            .fileExtension("tile.json")
            .build();

    private final Schema<?> schemaTileJson;
    private final Map<String, Schema<?>> referencedSchemas;
    private final FeaturesCoreProviders providers;
    private final SchemaInfo schemaInfo;

    @Inject
    public TileSetFormatTileJson(ClassSchemaCache classSchemaCache,
                                 FeaturesCoreProviders providers,
                                 SchemaInfo schemaInfo) {
        schemaTileJson = classSchemaCache.getSchema(TileJson.class);
        referencedSchemas = classSchemaCache.getReferencedSchemas(TileJson.class);
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
                .schema(schemaTileJson)
                .schemaRef(TileJson.SCHEMA_REF)
                .referencedSchemas(referencedSchemas)
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

        // TODO: add support for attribution and version (manage revisions to the data)
        return ImmutableTileJson.builder()
                                .tilejson("3.0.0")
                .name(apiData.getLabel())
                .description(apiData.getDescription())
                                .tiles(ImmutableList.of(getTilesUriTemplate(tileset)))
                                .bounds(TilesHelper.getBounds(tileset))
                                .minzoom(TilesHelper.getMinzoom(tileset))
                                .maxzoom(TilesHelper.getMaxzoom(tileset))
                                .center(TilesHelper.getCenter(tileset))
                                .vectorLayers(TilesHelper.getVectorLayers(apiData, collectionId, tileset.getTileMatrixSetId(), providers, schemaInfo))
                            .build();
    }

    private String getTilesUriTemplate(TileSet tileset) {
        return tileset.getLinks()
                      .stream()
                      .filter(link -> link.getRel().equalsIgnoreCase("item"))
                      .findFirst()
                      .map(Link::getHref)
                      .orElseThrow(() -> new RuntimeException("No tile URI template with link relation type 'item' found."))
                      .replace("{tileMatrixSetId}", tileset.getTileMatrixSetId())
                      .replace("{tileMatrix}", "{z}")
                      .replace("{tileRow}", "{y}")
                      .replace("{tileCol}", "{x}");
    }
}
