/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.oas30;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.openapi.OpenApiViewerResource;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Provides
@Instantiate
public class OpenApiFile implements ApiDefinitionFormatExtension {

    private static Logger LOGGER = LoggerFactory.getLogger(OpenApiFile.class);
    private static OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(MediaType.WILDCARD_TYPE)
            .build();

    @Requires
    private ExtendableOpenApiDefinition openApiDefinition;

    @Requires(optional = true)
    private OpenApiViewerResource openApiViewerResource;

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, Oas30Configuration.class);
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        if (path.equals("/api"))
            return null;

        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(new Schema())
                .schemaRef("#/components/schemas/any")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public String getPathPattern() {
        return "^/api/[^/]+$";
    }

    @Override
    public Response getApiDefinitionResponse(OgcApiApiDataV2 apiData,
                                             OgcApiRequestContext wfs3Request) {
        throw new NotAcceptableException();
    }

    @Override
    public Response getApiDefinitionFile(OgcApiApiDataV2 apiData,
                                         OgcApiRequestContext wfs3Request,
                                         String file) {
        LOGGER.debug("FILE {}", file);

        if (openApiViewerResource == null) {
            throw new NotFoundException();
        }

        // TODO: this also returns a 200 with an entity for non-existing files,  but there is no way to identify it here to throw a NotFoundException()
        Response response = openApiViewerResource.getFile(file);
        return response;
    }
}
