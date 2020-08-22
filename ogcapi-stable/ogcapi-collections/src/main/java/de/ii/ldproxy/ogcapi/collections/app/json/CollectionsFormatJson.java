/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.app.json;

import de.ii.ldproxy.ogcapi.collections.domain.Collections;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsFormatExtension;
import de.ii.ldproxy.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.SchemaGenerator;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {CollectionsFormatJson.class, CollectionsFormatExtension.class, FormatExtension.class, ApiExtension.class})
@Instantiate
public class CollectionsFormatJson implements CollectionsFormatExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schemaCollections;
    public final static String SCHEMA_REF_COLLECTIONS = "#/components/schemas/Collections";
    private final Schema schemaCollection;
    public final static String SCHEMA_REF_COLLECTION = "#/components/schemas/Collection";

    public CollectionsFormatJson() {
        schemaCollections = schemaGenerator.getSchema(Collections.class);
        schemaCollection = schemaGenerator.getSchema(OgcApiCollection.class);
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {

        // TODO add examples
        if (path.equals("/collections"))
                return new ImmutableApiMediaTypeContent.Builder()
                        .schema(schemaCollections)
                        .schemaRef(SCHEMA_REF_COLLECTIONS)
                        .ogcApiMediaType(MEDIA_TYPE)
                        .build();
        else if (path.matches("^/collections/[^//]+/?"))
            return new ImmutableApiMediaTypeContent.Builder()
                    .schema(schemaCollection)
                    .schemaRef(SCHEMA_REF_COLLECTION)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new RuntimeException("Unexpected path: " + path);
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Object getCollectionsEntity(Collections collections, OgcApi api, ApiRequestContext requestContext) {
        return collections;
    }

    @Override
    public Object getCollectionEntity(OgcApiCollection ogcApiCollection,
                                          OgcApi api,
                                          ApiRequestContext requestContext) {
        return ogcApiCollection;
    }
}
