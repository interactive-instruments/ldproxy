/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app.json;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.SchemaGenerator;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaInfo;
import de.ii.ldproxy.ogcapi.tiles.app.TilesHelper;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableTileJson;
import de.ii.ldproxy.ogcapi.tiles.domain.TileJson;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSet;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSetFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Optional;
import javax.ws.rs.core.MediaType;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate
public class TileSetFormatTileJson implements TileSetFormatExtension {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application","vnd.mapbox.tile+json"))
            .label("TileJSON")
            .parameter("tilejson")
            .build();

    private final Schema schemaTileJson;
    private final FeaturesCoreProviders providers;
    private final SchemaInfo schemaInfo;
    public final static String SCHEMA_REF_TILE_JSON = "#/components/schemas/TileJson";

    public TileSetFormatTileJson(@Requires SchemaGenerator schemaGenerator,
                                 @Requires FeaturesCoreProviders providers,
                                 @Requires SchemaInfo schemaInfo) {
        schemaTileJson = schemaGenerator.getSchema(TileJson.class);
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
                    .schemaRef(SCHEMA_REF_TILE_JSON)
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
