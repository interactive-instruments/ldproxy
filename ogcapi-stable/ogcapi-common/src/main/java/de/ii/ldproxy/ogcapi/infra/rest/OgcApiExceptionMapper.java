/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.xtraplatform.dropwizard.api.Jackson;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.Objects;


/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@Provider
public class OgcApiExceptionMapper extends LoggingExceptionMapper<Throwable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiExceptionMapper.class);

    @Requires
    Jackson jackson;

    @Context
    private UriInfo uriInfo;

    @Context
    private OgcApiRequestContext requestContext;

    @Validate
    void onStart() {
        LOGGER.debug("OGC API EXCEPTION WRITER");
    }

    @Override
    public Response toResponse(Throwable exception) {
        Response.Status responseStatus;
        MediaType mediaType = getResponseMediaType();
        OgcApiExceptionFormatExtension exceptionFormat = getExceptionFormat(mediaType);

        if (exception instanceof WebApplicationException) {
            final Response response = ((WebApplicationException) exception).getResponse();
            Response.Status.Family family = response.getStatusInfo().getFamily();
            if (family.equals(Response.Status.Family.CLIENT_ERROR)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Exception stacktrace:", exception);
                }
                return Response.fromResponse(response)
                        .type(mediaType)
                        .entity(exceptionFormat.getExceptionEntity(new OgcApiErrorMessage(response.getStatus(),
                                response.getStatusInfo().getReasonPhrase(),
                                exception.getMessage(),
                                uriInfo.getRequestUri().toString())))
                        .build();
            } else if (family.equals(Response.Status.Family.SERVER_ERROR)) {
                long id = logException(exception.getCause());
                return Response.serverError()
                        .type(mediaType)
                        .entity(exceptionFormat.getExceptionEntity(new OgcApiErrorMessage(response.getStatus(),
                                response.getStatusInfo().getReasonPhrase(),
                                String.format("There was an error processing your request, it has been logged. Error ID: %d", id),
                                uriInfo.getAbsolutePath().toString())))
                        .build();
            }
        } else if (exception instanceof IllegalArgumentException) {
            responseStatus = Response.Status.BAD_REQUEST;
            return Response.status(responseStatus)
                    .type(mediaType)
                    .entity(exceptionFormat.getExceptionEntity(new OgcApiErrorMessage(responseStatus.getStatusCode(),
                            responseStatus.getReasonPhrase(),
                            exception.getMessage(),
                            uriInfo.getAbsolutePath().toString())))
                    .build();
        } else if (exception instanceof UnsupportedOperationException) {
            responseStatus = Response.Status.UNSUPPORTED_MEDIA_TYPE;
            return Response.status(responseStatus)
                    .type(mediaType)
                    .entity(exceptionFormat.getExceptionEntity(new OgcApiErrorMessage(responseStatus.getStatusCode(),
                            responseStatus.getReasonPhrase(),
                            exception.getMessage(),
                            uriInfo.getAbsolutePath().toString())))
                    .build();
        }

        long id = logException(exception);
        responseStatus = Response.Status.INTERNAL_SERVER_ERROR;
        return Response.serverError()
                .type(mediaType)
                .entity(exceptionFormat.getExceptionEntity(new OgcApiErrorMessage(responseStatus.getStatusCode(),
                        responseStatus.getReasonPhrase(),
                        String.format("There was an error processing your request, it has been logged. Error ID: %d", id),
                        uriInfo.getAbsolutePath().toString())))
                .build();
    }

    private MediaType getResponseMediaType() {
        MediaType responseType = MediaType.valueOf("application/problem+json");
        List<String> typeParameter = uriInfo.getQueryParameters().get("f");
        if (Objects.nonNull(typeParameter) && !typeParameter.isEmpty() && "html".equals(typeParameter.get(0))) {
            responseType = MediaType.TEXT_HTML_TYPE;
        }
        return responseType;
    }

    private OgcApiExceptionFormatExtension getExceptionFormat(MediaType mediaType) {
        if (mediaType == MediaType.TEXT_HTML_TYPE) {
            return new OgcApiExceptionFormatHtml();
        }
        return new OgcApiExceptionFormatJson();
    }

}
