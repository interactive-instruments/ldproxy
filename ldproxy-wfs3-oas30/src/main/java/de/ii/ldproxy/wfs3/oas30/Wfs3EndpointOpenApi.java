/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.oas30;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiContext;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiEndpointExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.xtraplatform.openapi.DynamicOpenApi;
import de.ii.xtraplatform.openapi.OpenApiViewerResource;
import de.ii.xtraplatform.service.api.Service;
import io.dropwizard.jersey.caching.CacheControl;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointOpenApi implements ConformanceClass, OgcApiEndpointExtension {

    private static Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointOpenApi.class);
    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("api")
            .build();
    private static final ImmutableSet<OgcApiMediaType> API_MEDIA_TYPES = ImmutableSet.of(
            new ImmutableOgcApiMediaType.Builder()
                    .main(new MediaType("application", "vnd.oai.openapi+json"))
                    .build(),
            new ImmutableOgcApiMediaType.Builder()
                    .main(MediaType.TEXT_HTML_TYPE)
                    .build(),
            // the following is needed, because the HTML requires sub-resources
            new ImmutableOgcApiMediaType.Builder()
                    .main(new MediaType("application", "javascript"))
                    .build(),
            new ImmutableOgcApiMediaType.Builder()
                    .main(MediaType.APPLICATION_JSON_TYPE)
                    .build(),
            new ImmutableOgcApiMediaType.Builder()
                    .main(new MediaType("image", "*"))
                    .build(),
            new ImmutableOgcApiMediaType.Builder()
                    .main(new MediaType("text", "css"))
                    .build()
    );

    @Requires(optional = true)
    private OpenApiViewerResource openApiViewerResource;

    @Requires
    private ExtendableOpenApiDefinition openApiDefinition;

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/oas30";
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData dataset) {
        return API_MEDIA_TYPES;
    }

    @Override
    public boolean isEnabledForDataset(OgcApiDatasetData datasetData) {
        return isExtensionEnabled(datasetData, Oas30Configuration.class);
    }

    @GET
    @Produces({MediaType.TEXT_HTML})
    public Response getApiDescription(@Context Service service, @Context OgcApiRequestContext wfs3Request,
                                      @Context HttpHeaders headers) throws Exception {
        if (!wfs3Request.getUriCustomizer()
                        .getPath()
                        .endsWith("/")) {
            return Response
                    .status(Response.Status.MOVED_PERMANENTLY)
                    .location(wfs3Request.getUriCustomizer()
                                         .copy()
                                         .ensureTrailingSlash()
                                         .build())
                    .build();
        }

        LOGGER.debug("MIME {} {}", "HTML", headers.getHeaderString("Accept"));
        return openApiViewerResource.getFile("index.html");
    }

    @GET
    @Produces({"application/vnd.oai.openapi+json;version=3.0", MediaType.APPLICATION_JSON})
    //@Operation(summary = "the API description - this document", tags = {"Capabilities"}, parameters = {@Parameter(name = "f")})
    public Response getApiDescriptionJson(@Context OgcApiDataset service,
                                          @Context OgcApiRequestContext wfs3Request) throws Exception {
        LOGGER.debug("MIME {})", "JSON");
        return openApiDefinition.getOpenApi("json", wfs3Request.getUriCustomizer()
                                                               .copy(), service.getData());
    }


    @GET
    @Produces({DynamicOpenApi.YAML})
    //@Operation(summary = "the API description - this document", tags = {"Capabilities"}, parameters = {@Parameter(name = "f")})
    public Response getApiDescriptionYaml(@Context OgcApiDataset service,
                                          @Context OgcApiRequestContext wfs3Request) throws Exception {
        LOGGER.debug("MIME {})", "YAML");
        return openApiDefinition.getOpenApi("yaml", wfs3Request.getUriCustomizer()
                                                               .copy(), service.getData());
    }

    @GET
    @Path("/{file}")
    @Produces({MediaType.WILDCARD})
    @CacheControl(maxAge = 3600)
    public Response getFile(@PathParam("file") String file) {
        LOGGER.debug("FILE {})", file);

        if (openApiViewerResource == null) {
            throw new NotFoundException();
        }

        return openApiViewerResource.getFile(file);
    }
}
