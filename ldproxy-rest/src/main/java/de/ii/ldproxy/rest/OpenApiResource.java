/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.rest;


/**
 * @author zahnen
 */


import de.ii.ldproxy.service.LdProxyService;
import de.ii.xsf.dropwizard.api.Dropwizard;
import de.ii.xsf.dropwizard.api.Jackson;
import de.ii.xsf.dropwizard.cfg.JacksonProvider;
import de.ii.xtraplatform.openapi.DynamicOpenApi;
import de.ii.xtraplatform.openapi.OpenApiViewerResource;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.core.filter.OpenAPISpecFilter;
import io.swagger.oas.annotations.Operation;
import io.swagger.oas.annotations.Parameter;
import io.swagger.oas.annotations.media.Content;
import io.swagger.oas.annotations.media.Schema;
import io.swagger.oas.annotations.responses.ApiResponse;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Component
@Provides(specifications = {OpenApiResource.class})
@Instantiate
//@Path("/services/{id}/api")
public class OpenApiResource {

    private static Logger LOGGER = LoggerFactory.getLogger(OpenApiResource.class);

    //@Requires
    //private DynamicOpenApi openApi;

    @Requires(optional = true)
    private OpenApiViewerResource openApiViewerResource;

    private Wfs3SpecFilter wfs3SpecFilter;

    OpenApiResource(@Requires Jackson jackson, @Requires Dropwizard dropwizard) {
        this.wfs3SpecFilter = new Wfs3SpecFilter(jackson.getDefaultObjectMapper(), dropwizard.getExternalUrl());
    }

    public void setService(LdProxyService service) {
        wfs3SpecFilter.setService(service);
    }

    @GET
    @Produces({MediaType.TEXT_HTML})
    @Operation(
            summary = "the API description - this document",
            tags = {"Capabilities"},
            parameters = {@Parameter(name = "f")},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "The formal documentation of this API according to the OpenAPI specification, version 3.0. I.e., this document.",
                            content = {
                                    @Content(
                                            mediaType = "application/openapi+json;version=3.0",
                                            schema = @Schema(type = "object")
                                    ),
                                    @Content(
                                            mediaType = "text/html",
                                            schema = @Schema(type = "string")
                                    )
                            }
                    ),
                    @ApiResponse(
                            description = "An error occured.",
                            content = {
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(ref = "#/components/schemas/exception")
                                    ),
                                    @Content(
                                            mediaType = "application/xml",
                                            schema = @Schema(ref = "#/components/schemas/exception")
                                    ),
                                    @Content(
                                            mediaType = "text/html",
                                            schema = @Schema(type = "string")
                                    )
                            }
                    )
            })
    public Response getApiDescription(@Context HttpHeaders headers, @Context UriInfo uriInfo) throws Exception {
        LOGGER.debug("MIME {} {}", "HTML", headers.getHeaderString("Accept"));
        return openApiViewerResource.getFile("index.html");
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    //@Operation(summary = "the API description - this document", tags = {"Capabilities"}, parameters = {@Parameter(name = "f")})
    public Response getApiDescriptionJson(@Context HttpHeaders headers, @Context UriInfo uriInfo) throws Exception {
        LOGGER.debug("MIME {})", "JSON");
        //return openApi.getOpenApi(headers, uriInfo, "json", wfs3SpecFilter);
        return wfs3SpecFilter.getOpenApi("json");
    }


    @GET
    @Produces({DynamicOpenApi.YAML})
    //@Operation(summary = "the API description - this document", tags = {"Capabilities"}, parameters = {@Parameter(name = "f")})
    public Response getApiDescriptionYaml(@Context HttpHeaders headers, @Context UriInfo uriInfo) throws Exception {
        LOGGER.debug("MIME {})", "YAML");
        //return openApi.getOpenApi(headers, uriInfo, "yaml", wfs3SpecFilter);
        return wfs3SpecFilter.getOpenApi("yaml");
    }

    @GET
    @Path("/{file}")
    @Operation
    @CacheControl(maxAge = 3600)
    public Response getFile(@PathParam("file") String file) {
        LOGGER.debug("FILE {})", file);

        if (openApiViewerResource == null) {
            throw new NotFoundException();
        }

        return openApiViewerResource.getFile(file);
    }

}
