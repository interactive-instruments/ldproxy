/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.common.domain.QueryParameterF;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @langEn Todo
 * @langDe Todo
 * @name fTile
 * @endpoints Tile
 */

@Singleton
@AutoBind
public class QueryParameterFTile extends QueryParameterF {

    @Inject
    protected QueryParameterFTile(ExtensionRegistry extensionRegistry, SchemaValidator schemaValidator) {
        super(extensionRegistry, schemaValidator);
    }

    @Override
    public String getId() {
        return "fTile";
    }

    @Override
    protected boolean matchesPath(String definitionPath) {
        return (definitionPath.equals("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}") ||
            definitionPath.equals("/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"));
    }

    @Override
    protected Class<? extends FormatExtension> getFormatClass() {
        return TileFormatExtension.class;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    // TODO: remove the getSchema methods again, this is a temporary solution/hack to remove any MapTile formats

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey("*")) {
            List<String> fEnum = new ArrayList<>();
            extensionRegistry.getExtensionsForType(getFormatClass())
                .stream()
                .filter(f -> !f.getClass().getSimpleName().startsWith("Map")) // TODO
                .filter(f -> f.isEnabledForApi(apiData))
                .filter(f -> !f.getMediaType().parameter().equals("*"))
                .forEach(f -> fEnum.add(f.getMediaType().parameter()));
            schemaMap.get(apiHashCode).put("*", new StringSchema()._enum(fEnum));
        }
        return schemaMap.get(apiHashCode).get("*");
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
            List<String> fEnum = new ArrayList<>();
            extensionRegistry.getExtensionsForType(getFormatClass())
                .stream()
                .filter(f -> !f.getClass().getSimpleName().startsWith("Map")) // TODO
                .filter(f -> f.isEnabledForApi(apiData, collectionId))
                .filter(f -> !f.getMediaType().parameter().equals("*"))
                .forEach(f -> fEnum.add(f.getMediaType().parameter()));
            schemaMap.get(apiHashCode).put(collectionId, new StringSchema()._enum(fEnum));
        }
        return schemaMap.get(apiHashCode).get(collectionId);
    }

}
