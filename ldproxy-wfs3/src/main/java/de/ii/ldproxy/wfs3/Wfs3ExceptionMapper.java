/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.ii.xsf.dropwizard.api.Jackson;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@Provider
@Produces({MediaType.WILDCARD})
public class Wfs3ExceptionMapper extends LoggingExceptionMapper<Throwable> implements MessageBodyWriter<Wfs3ExceptionMapper.WfsErrorMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3ExceptionMapper.class);

    /*
    new ErrorEntityWriter<ErrorMessage,View>(MediaType.TEXT_HTML_TYPE, View.class) {
    @Override
    protected View getRepresentation(ErrorMessage errorMessage) {
        return new ErrorView(errorMessage);
    }
}
     */

    @Requires
    Jackson jackson;

    @Validate
    void onStart() {
        LOGGER.debug("WFS3 EXCEPTION WRITER");
    }

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException) {
            final Response response = ((WebApplicationException) exception).getResponse();
            if (exception.getCause() != null) {
                return Response.fromResponse(response)
                               .type(MediaType.APPLICATION_JSON_TYPE)
                               .entity(new WfsErrorMessage(response.getStatus(), exception.getLocalizedMessage(), exception.getCause().getLocalizedMessage()))
                               .build();
            }
            return Response.fromResponse(response)
                           .type(MediaType.APPLICATION_JSON_TYPE)
                           .entity(new WfsErrorMessage(response.getStatus(), exception.getLocalizedMessage()))
                           .build();
        }

        LOGGER.debug("UNEXPECTED EXCEPTION", exception);

        return super.toResponse(exception);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type.equals(Wfs3ExceptionMapper.WfsErrorMessage.class);
    }

    @Override
    public long getSize(WfsErrorMessage e, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(WfsErrorMessage e, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        //TODO: f=html not working, receives json
        switch (mediaType.toString()) {
            case Wfs3MediaTypes.HTML:
                //TODO: Wfs3ExceptionView, might delegate to WfsOutputFormatHtml
                //return e.getResponse();
            case Wfs3MediaTypes.JSON:
            case Wfs3MediaTypes.GEO_JSON:
                jackson.getDefaultObjectMapper()
                       .writeValue(entityStream, e);
                break;
            default:
                //return e.getResponse();
        }
    }

    static class WfsErrorMessage extends ErrorMessage {

        public WfsErrorMessage(String message) {
            super(message);
        }

        public WfsErrorMessage(int code, String message) {
            super(code, message);
        }

        public WfsErrorMessage(int code, String message, String details) {
            super(code, message, details);
        }

        @JsonProperty("description")
        @Override
        public String getMessage() {
            return super.getMessage();
        }
    }
}
