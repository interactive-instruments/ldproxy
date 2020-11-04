/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ApiErrorMessage;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ExceptionFormatExtension;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatNotSupportedException;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.glassfish.jersey.message.internal.MessageBodyProviderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
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

        String msg = exception.getMessage();
        if (Objects.isNull(msg))
            msg = exception.getClass().getSimpleName() + " at " + exception.getStackTrace()[0].toString();
        String msgCause = Objects.nonNull(exception.getCause()) ? exception.getCause().getMessage() : "";
        if (Objects.isNull(msgCause))
            msgCause = exception.getCause().getClass().getSimpleName() + " at " + exception.getCause().getStackTrace()[0].toString();

        if (exception instanceof FormatNotSupportedException) {
            responseStatus = Response.Status.NOT_ACCEPTABLE;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Client error, HTTP status {}, Request path {}: {}", responseStatus.getStatusCode(), getRequestPath(true), exception.getMessage());
            }
            return Response.status(responseStatus)
                           .type(exceptionFormat.getMediaType().type())
                           .entity(exceptionFormat.getExceptionEntity(new ApiErrorMessage(responseStatus.getStatusCode(),
                                                                                          responseStatus.getReasonPhrase(),
                                                                                          msg)))
                           .build();
        } else if (exception instanceof InternalServerErrorException &&
                   Objects.requireNonNullElse(exception.getCause(),exception) instanceof MessageBodyProviderNotFoundException) {
            // this catches exceptions when the API catalog is requested and the requested format cannot be written
            responseStatus = Response.Status.NOT_ACCEPTABLE;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Client error, HTTP status {}, Request path {}: {}", responseStatus.getStatusCode(), getRequestPath(true), exception.getCause().getMessage());
            }
            return Response.status(responseStatus)
                           .type(exceptionFormat.getMediaType().type())
                           .entity(exceptionFormat.getExceptionEntity(new ApiErrorMessage(responseStatus.getStatusCode(),
                                                                                          responseStatus.getReasonPhrase(),
                                                                                          msgCause)))
                           .build();
        } else if (exception instanceof WebApplicationException) {
            final Response response = ((WebApplicationException) exception).getResponse();
            Response.Status.Family family = response.getStatusInfo().getFamily();
            if (family.equals(Response.Status.Family.REDIRECTION)) {
                return response;
            } else if (family.equals(Response.Status.Family.CLIENT_ERROR)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Client error, HTTP status {}, Request path {}: {}", response.getStatus(), getRequestPath(true), exception.getMessage());
                }
                return Response.fromResponse(response)
                        .type(exceptionFormat.getMediaType().type())
                        .entity(exceptionFormat.getExceptionEntity(new ApiErrorMessage(response.getStatus(),
                                                                                    response.getStatusInfo().getReasonPhrase(),
                                                                                    msg)))
                        .build();
            } else if (family.equals(Response.Status.Family.SERVER_ERROR)) {
                long id = logException(Objects.nonNull(exception.getCause()) ? exception.getCause() : exception);
                return Response.serverError()
                        .type(exceptionFormat.getMediaType().type())
                        .entity(exceptionFormat.getExceptionEntity(new ApiErrorMessage(response.getStatus(),
                                                                                    response.getStatusInfo().getReasonPhrase(),
                                                                                    String.format("There was an error processing your request, it has been logged. Error ID: %016x", id))))
                        .build();
            }
        } else if (exception instanceof IllegalArgumentException) {
            responseStatus = Response.Status.BAD_REQUEST;
            String message = msgCause.isEmpty() ? msg : String.format("%s: %s", msg, msgCause);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Client error, HTTP status {}, Request path {}: {}", responseStatus.getStatusCode(), getRequestPath(true), message);
            }
            return Response.status(responseStatus)
                    .type(exceptionFormat.getMediaType().type())
                    .entity(exceptionFormat.getExceptionEntity(new ApiErrorMessage(responseStatus.getStatusCode(),
                                                                                responseStatus.getReasonPhrase(),
                                                                                message)))
                    .build();
        }

        long id = logException(exception);
        responseStatus = Response.Status.INTERNAL_SERVER_ERROR;
        return Response.serverError()
                .type(exceptionFormat.getMediaType().type())
                .entity(exceptionFormat.getExceptionEntity(new ApiErrorMessage(responseStatus.getStatusCode(),
                                                                            responseStatus.getReasonPhrase(),
                                                                            String.format("There was an error processing your request, it has been logged. Error ID: %016x", id))))
                .build();
    }

    protected long logException(Throwable exception) {
        final long id = ThreadLocalRandom.current().nextLong();
        String message = exception.getMessage();
        if (Objects.isNull(message))
            message = exception.getClass().getSimpleName() + " at " + exception.getStackTrace()[0].toString();
        LOGGER.error(String.format("Server Error with ID %016x: {}", id), message);
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Stacktrace:", exception);
        }
        return id;
    }

    private String getRequestPath(boolean queryParameters) {
        // TODO we do not have access to the externalUrl, so this can only be used for responses
        String s = queryParameters ? uriInfo.getRequestUri().toString() :
                                     uriInfo.getAbsolutePath().toString();
        return s.substring(s.lastIndexOf("/rest/services") + 14);
    }

}
