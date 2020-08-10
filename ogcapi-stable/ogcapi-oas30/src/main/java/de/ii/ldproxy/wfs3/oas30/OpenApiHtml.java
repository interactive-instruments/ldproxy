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
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URISyntaxException;

@Component
@Provides
@Instantiate
public class OpenApiHtml implements ApiDefinitionFormatExtension {

    private static Logger LOGGER = LoggerFactory.getLogger(OpenApiHtml.class);
    private static OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .label("HTML")
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
        if (path.startsWith("/api/"))
            return null;

        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(new StringSchema().example("<html>...</html>"))
                .schemaRef("#/components/schemas/htmlSchema")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public Response getApiDefinitionResponse(OgcApiApiDataV2 apiData,
                                             OgcApiRequestContext wfs3Request) {
        if (!wfs3Request.getUriCustomizer()
                        .getPath()
                        .endsWith("/")) {
            try {
                return Response
                        .status(Response.Status.MOVED_PERMANENTLY)
                        .location(wfs3Request.getUriCustomizer()
                                .copy()
                                .ensureTrailingSlash()
                                .build())
                        .build();
            } catch (URISyntaxException ex) {
                throw new RuntimeException("Invalid URI: " + ex.getMessage(), ex);
            }
        }

        if (openApiViewerResource == null) {
            throw new NullPointerException();
        }

        return openApiViewerResource.getFile("index.html");
    }
}
