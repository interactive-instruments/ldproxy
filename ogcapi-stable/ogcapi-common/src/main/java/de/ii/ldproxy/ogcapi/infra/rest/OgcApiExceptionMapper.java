/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import de.ii.xtraplatform.dropwizard.api.Jackson;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import io.dropwizard.views.View;
import io.dropwizard.views.mustache.MustacheViewRenderer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@Provider
@Produces({MediaType.WILDCARD})
public class OgcApiExceptionMapper extends LoggingExceptionMapper<Throwable> implements MessageBodyWriter<OgcApiErrorMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiExceptionMapper.class);

    @Requires
    Jackson jackson;

    @Context
    private UriInfo uriInfo;

    @Validate
    void onStart() {
        LOGGER.debug("OGC API EXCEPTION WRITER");
    }

    @Override
    public Response toResponse(Throwable exception) {
        String mediaType = getResponseMediaType();
        if (exception instanceof WebApplicationException) {
            final Response response = ((WebApplicationException) exception).getResponse();
            Response.Status.Family family = response.getStatusInfo().getFamily();
            if (family.equals(Response.Status.Family.CLIENT_ERROR)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Exception stacktrace:", exception);
                }
                return Response.fromResponse(response)
                        .type(mediaType)
                        .entity(new OgcApiErrorMessage(response.getStatus(),
                                response.getStatusInfo().getReasonPhrase(),
                                exception.getMessage(),
                                uriInfo.getRequestUri().toString()))
                        .build();
            }
        }
        long id = logException(exception);
        int status = 500;
        return Response.serverError()
                .type(mediaType)
                .entity(new OgcApiErrorMessage(status,
                        Response.Status.fromStatusCode(status).getReasonPhrase(),
                        String.format("There was an error processing your request, it has been logged. Error ID: %d", id),
                        uriInfo.getAbsolutePath().toString()))
                .build();
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type.equals(OgcApiErrorMessage.class);
    }

    @Override
    public long getSize(OgcApiErrorMessage e, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(OgcApiErrorMessage e, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        if (MediaType.TEXT_HTML.equals(mediaType.toString())) {
            View view = new OgcApiErrorView(e.getMessage(), e.getCode(), e.getDetails(), e.getInstance());
            MustacheViewRenderer renderer = new MustacheViewRenderer();
            renderer.render(view, null, entityStream);
        } else {
            jackson.getDefaultObjectMapper()
                    .writeValue(entityStream, e);
        }
    }

    private String getResponseMediaType() {
        String responseType = "application/problem+json";
        List<String> typeParameter = uriInfo.getQueryParameters().get("f");
        if (Objects.nonNull(typeParameter) && !typeParameter.isEmpty() && "html".equals(typeParameter.get(0))) {
            responseType = "text/html";
        }
        return responseType;
    }

}
