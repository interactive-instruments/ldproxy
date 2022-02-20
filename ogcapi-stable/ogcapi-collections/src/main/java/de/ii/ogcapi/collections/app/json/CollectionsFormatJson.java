/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.app.json;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.Collections;
import de.ii.ogcapi.collections.domain.CollectionsFormatExtension;
import de.ii.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.SchemaGenerator;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class CollectionsFormatJson implements CollectionsFormatExtension, ConformanceClass {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schemaCollections;
    public final static String SCHEMA_REF_COLLECTIONS = "#/components/schemas/Collections";
    private final Schema schemaCollection;
    public final static String SCHEMA_REF_COLLECTION = "#/components/schemas/Collection";

    @Inject
    public CollectionsFormatJson(SchemaGenerator schemaGenerator) {
        schemaCollections = schemaGenerator.getSchema(Collections.class);
        schemaCollection = schemaGenerator.getSchema(OgcApiCollection.class);
    }

    @Override
    public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-common-2/0.0/conf/json");
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
