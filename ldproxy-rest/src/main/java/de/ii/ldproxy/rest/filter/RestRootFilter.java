/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.rest.filter;

import com.google.common.io.Resources;
import de.ii.ldproxy.output.html.DatasetView;
import de.ii.xsf.logging.XSFLogger;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.eclipse.jetty.http.MimeTypes;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;

import javax.activation.MimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@PreMatching
public class RestRootFilter implements ContainerResponseFilter {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(RestRootFilter.class);

    // TODO: verify, adjust for manager
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

        if (requestContext.getUriInfo().getPath().endsWith("app/css/app3.css")) {
            writeContent(responseContext, "/css/app.css", new MediaType("text", "css", "utf-8"));
        } else if (requestContext.getUriInfo().getPath().endsWith("app/js/featureCollection.js")) {
            writeContent(responseContext, "/js/featureCollection.js", new MediaType("application", "javascript"));
        } else if (requestContext.getUriInfo().getPath().endsWith("app/js/featureDetails.js")) {
            writeContent(responseContext, "/js/featureDetails.js", new MediaType("application", "javascript"));
        } else if (requestContext.getUriInfo().getPath().contains("app/js/")) {
            String p = requestContext.getUriInfo().getPath().substring(requestContext.getUriInfo().getPath().indexOf("app/js/")+7);
            writeContent(responseContext, "/js/" + p, new MediaType("application", "javascript"));
        } else if (requestContext.getUriInfo().getPath().endsWith("favicon.ico")) {
            writeContent(responseContext, "/img/favicon.ico", new MediaType("image", "x-icon"));
        }
    }

    private void writeContent(ContainerResponseContext responseContext, final String file, final MediaType mimeType) {
        //response.reset();

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                Resources.asByteSource(Resources.getResource(DatasetView.class, file)).copyTo(output);
            }
        };

        //Response r = Response.ok(stream, mimeType).build();

        //response.setResponse(r);
        responseContext.setStatus(200);
        responseContext.getHeaders().putSingle("Cache-Control", "max-age=3600");
        responseContext.setEntity(stream, null, mimeType);
    }
}
