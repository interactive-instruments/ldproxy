/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.app;

import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.resources.domain.ResourceFormatExtension;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.Schema;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
@AutoBind
public class ResourceFormatAny implements ResourceFormatExtension {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.WILDCARD_TYPE)
            .build();

    private final Schema schemaResource = new BinarySchema();
    public final static String SCHEMA_REF_RESOURCE = "#/components/schemas/Resource";

    @Inject
    ResourceFormatAny() {
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {

        // TODO add examples
        if (path.equals("/resources/{resourceId}"))
            return new ImmutableApiMediaTypeContent.Builder()
                    .schema(schemaResource)
                    .schemaRef(SCHEMA_REF_RESOURCE)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new RuntimeException("Unexpected path: " + path);
    }

    @Override
    public ApiMediaTypeContent getRequestContent(OgcApiDataV2 apiData, String path, HttpMethods method) {

        // TODO add examples
        if (path.equals("/resources/{resourceId}"))
            return new ImmutableApiMediaTypeContent.Builder()
                    .schema(schemaResource)
                    .schemaRef(SCHEMA_REF_RESOURCE)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new RuntimeException("Unexpected path: " + path);
    }

    @Override
    public Object getResourceEntity(byte[] resource, String resourceId, OgcApiDataV2 apiData, ApiRequestContext requestContext) {
        return resource;
    }

    @Override
    public Response putResource(Path resourcesStore, byte[] resource, String resourceId, OgcApiDataV2 apiData, ApiRequestContext requestContext) throws IOException {

        final String apiId = apiData.getId();
        Path apiDir = resourcesStore.resolve(apiId);
        Files.createDirectories(apiDir);

        Path resourceFile = apiDir.resolve(resourceId);

        try {
            Files.write(resourceFile, resource);
        } catch (IOException e) {
            throw new RuntimeException("Could not PUT resource: " + resourceId);
        }

        return Response.noContent()
                       .build();
    }
}
