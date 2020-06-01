/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.nearby;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.infra.json.SchemaGenerator;
import de.ii.ldproxy.ogcapi.infra.json.SchemaGeneratorImpl;
import de.ii.ldproxy.target.geojson.OgcApiOutputFormatJson;
import de.ii.ldproxy.wfs3.oas30.OpenApiExtension;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class OpenApiNearby implements OpenApiExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    @Override
    public int getSortPriority() {
        return 10000;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, NearbyConfiguration.class);
    }

    @Override
    public OpenAPI process(OpenAPI openAPI, OgcApiApiDataV2 apiData) {

        if (isEnabledForApi(apiData)) {
            ObjectSchema collectionInfo = (ObjectSchema) openAPI.getComponents()
                    .getSchemas()
                    .get(OgcApiOutputFormatJson.SCHEMA_REF_COLLECTION.substring(OgcApiOutputFormatJson.SCHEMA_REF_COLLECTION.lastIndexOf("/")+1));
            if (collectionInfo!=null)
                collectionInfo.getProperties()
                        .put("relations",
                                new ArraySchema().items(schemaGenerator.getSchema(NearbyConfiguration.Relation.class)));
        }

        return openAPI;
    }
}
