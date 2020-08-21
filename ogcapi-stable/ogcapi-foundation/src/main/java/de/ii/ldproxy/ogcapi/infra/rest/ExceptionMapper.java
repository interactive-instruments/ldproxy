/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import org.apache.felix.ipojo.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;


@Component
@Provides
@Instantiate
@Provider
public class ExceptionMapper extends LoggingExceptionMapper<Throwable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionMapper.class);

    @Requires
    ExtensionRegistry extensionRegistry;

    @Context
    UriInfo uriInfo;

    @Context
    Request request;

    @Context
    HttpHeaders httpHeaders;

    private final ContentNegotiation contentNegotiation;

    public ExceptionMapper() {
        contentNegotiation = new ContentNegotiation();
    }

    @Validate
    void onStart() {
        LOGGER.debug("OGC API EXCEPTION WRITER");
    }

    @Override
    public Response toResponse(Throwable exception) {
        Response.Status responseStatus;

        // content negotiation for the error response
        ImmutableSet<ApiMediaType> supportedMediaTypes = extensionRegistry.getExtensionsForType(ExceptionFormatExtension.class)
                                                                          .stream()
                                                                          .map(format -> format.getMediaType())
                                                                          .collect(ImmutableSet.toImmutableSet());
        Optional<ApiMediaType> mediaType = contentNegotiation.negotiate(request, httpHeaders, uriInfo, supportedMediaTypes);
        ExceptionFormatExtension exceptionFormat =
                mediaType.isPresent() ?
                extensionRegistry.getExtensionsForType(ExceptionFormatExtension.class)
                                 .stream()
                                 .filter(format -> mediaType.get() == format.getMediaType())
                                 .findFirst()
                                 .get() :
                extensionRegistry.getExtensionsForType(ExceptionFormatExtension.class).get(0);

        if (exception instanceof WebApplicationException) {
            final Response response = ((WebApplicationException) exception).getResponse();
            Response.Status.Family family = response.getStatusInfo().getFamily();
            if (family.equals(Response.Status.Family.REDIRECTION)) {
                return response;
            } else if (family.equals(Response.Status.Family.CLIENT_ERROR)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Client error, HTTP status {}, Request URI {}: {}", response.getStatus(), uriInfo.getRequestUri().toString(), exception.getMessage());
                }
                return Response.fromResponse(response)
                        .type(exceptionFormat.getMediaType().type())
                        .entity(exceptionFormat.getExceptionEntity(new ApiErrorMessage(response.getStatus(),
                                                                                    response.getStatusInfo().getReasonPhrase(),
                                                                                    exception.getMessage(),
                                                                                    uriInfo.getRequestUri().toString())))
                        .build();
            } else if (family.equals(Response.Status.Family.SERVER_ERROR)) {
                long id = logException(Objects.nonNull(exception.getCause()) ? exception.getCause() : exception);
                return Response.serverError()
                        .type(exceptionFormat.getMediaType().type())
                        .entity(exceptionFormat.getExceptionEntity(new ApiErrorMessage(response.getStatus(),
                                                                                    response.getStatusInfo().getReasonPhrase(),
                                                                                    String.format("There was an error processing your request, it has been logged. Error ID: %016x", id),
                                                                                    uriInfo.getAbsolutePath().toString())))
                        .build();
            }
        } else if (exception instanceof IllegalArgumentException) {
            responseStatus = Response.Status.BAD_REQUEST;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Client error, HTTP status {}, Request URI {}: {}", responseStatus.getStatusCode(), uriInfo.getRequestUri().toString(), exception.getMessage());
            }
            return Response.status(responseStatus)
                    .type(exceptionFormat.getMediaType().type())
                    .entity(exceptionFormat.getExceptionEntity(new ApiErrorMessage(responseStatus.getStatusCode(),
                                                                                responseStatus.getReasonPhrase(),
                                                                                exception.getMessage(),
                                                                                uriInfo.getAbsolutePath().toString())))
                    .build();
        } else if (exception instanceof FormatNotSupportedException) {
            responseStatus = Response.Status.NOT_ACCEPTABLE;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Client error, HTTP status {}, Request URI {}: {}", responseStatus.getStatusCode(), uriInfo.getRequestUri().toString(), exception.getMessage());
            }
            return Response.status(responseStatus)
                    .type(exceptionFormat.getMediaType().type())
                    .entity(exceptionFormat.getExceptionEntity(new ApiErrorMessage(responseStatus.getStatusCode(),
                                                                                responseStatus.getReasonPhrase(),
                                                                                exception.getMessage(),
                                                                                uriInfo.getAbsolutePath().toString())))
                    .build();
        }

        long id = logException(exception);
        responseStatus = Response.Status.INTERNAL_SERVER_ERROR;
        return Response.serverError()
                .type(exceptionFormat.getMediaType().type())
                .entity(exceptionFormat.getExceptionEntity(new ApiErrorMessage(responseStatus.getStatusCode(),
                                                                            responseStatus.getReasonPhrase(),
                                                                            String.format("There was an error processing your request, it has been logged. Error ID: %016x", id),
                                                                            uriInfo.getAbsolutePath().toString())))
                .build();
    }

    protected long logException(Throwable exception) {
        final long id = ThreadLocalRandom.current().nextLong();
        LOGGER.error(String.format("Server Error with ID %016x: {}", id), exception.getMessage());
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Stacktrace:", exception);
        }
        return id;
    }
}
