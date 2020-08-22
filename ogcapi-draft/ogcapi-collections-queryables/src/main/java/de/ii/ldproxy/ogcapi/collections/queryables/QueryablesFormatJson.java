/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.queryables;

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
public class QueryablesFormatJson implements QueryablesFormatExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    private final Schema schema;
    public final static String SCHEMA_REF = "#/components/schemas/Queryables";

    public QueryablesFormatJson() {
        schema = schemaGenerator.getSchema(Queryables.class);
    }

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(SCHEMA_REF)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public Object getEntity(Queryables queryables, String collectionId, OgcApi api, ApiRequestContext requestContext) {
        return queryables;
    }
}
