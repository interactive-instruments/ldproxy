/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.oas30;

import de.ii.ldproxy.wfs3.api.Wfs3ConformanceClass;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
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
public class Wfs3EndpointOpenApi implements Wfs3ConformanceClass, Wfs3EndpointExtension {

    private static Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointOpenApi.class);

    @Requires(optional = true)
    private OpenApiViewerResource openApiViewerResource;

    @Requires
    private ExtendableOpenApiDefinition openApiDefinition;

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/wfs-1/3.0/req/oas30";
    }

    @Override
    public boolean isConformanceEnabledForService(Wfs3ServiceData serviceData) {
        if (isExtensionEnabled(serviceData, Oas30Configuration.class)) {
            return true;
        }
        return false;
    }

    @Override
    public String getPath() {
        return "api";
    }

    @Override
    public boolean isEnabledForService(Wfs3ServiceData serviceData) {
        if (!isExtensionEnabled(serviceData, Oas30Configuration.class)) {
            throw new NotFoundException();
        }
        return true;
    }

    @GET
    @Produces({MediaType.TEXT_HTML})
    public Response getApiDescription(@Context Service service, @Context Wfs3RequestContext wfs3Request, @Context HttpHeaders headers) throws Exception {
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
    @Produces({"application/openapi+json;version=3.0", MediaType.APPLICATION_JSON})
    //@Operation(summary = "the API description - this document", tags = {"Capabilities"}, parameters = {@Parameter(name = "f")})
    public Response getApiDescriptionJson(@Context Service service, @Context Wfs3RequestContext wfs3Request) throws Exception {
        LOGGER.debug("MIME {})", "JSON");
        return openApiDefinition.getOpenApi("json", wfs3Request.getUriCustomizer()
                                                               .copy(), (Wfs3ServiceData) service.getData());
    }


    @GET
    @Produces({DynamicOpenApi.YAML})
    //@Operation(summary = "the API description - this document", tags = {"Capabilities"}, parameters = {@Parameter(name = "f")})
    public Response getApiDescriptionYaml(@Context Service service, @Context Wfs3RequestContext wfs3Request) throws Exception {
        LOGGER.debug("MIME {})", "YAML");
        return openApiDefinition.getOpenApi("yaml", wfs3Request.getUriCustomizer()
                                                               .copy(), (Wfs3ServiceData) service.getData());
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
