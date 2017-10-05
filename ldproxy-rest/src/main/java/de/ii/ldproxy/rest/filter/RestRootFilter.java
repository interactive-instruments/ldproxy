/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.rest.filter;

import com.google.common.io.Resources;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import de.ii.ldproxy.output.html.DatasetView;
import de.ii.xsf.logging.XSFLogger;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import javax.ws.rs.WebApplicationException;
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

public class RestRootFilter implements ContainerResponseFilter {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(RestRootFilter.class);

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {

        if (request.getPath().endsWith("services/app/css/app.css")) {
            writeContent(response, "/css/app.css", "text/css; charset=utf-8");
        } else if (request.getPath().endsWith("services/app/js/featureCollection.js")) {
            writeContent(response, "/js/featureCollection.js", "application/javascript");
        } else if (request.getPath().endsWith("services/app/js/featureDetails.js")) {
            writeContent(response, "/js/featureDetails.js", "application/javascript");
        } else if (request.getPath().endsWith("services/favicon.ico")) {
            writeContent(response, "/img/favicon.ico", "image/x-icon");
        }

        return response;
    }

    private void writeContent(ContainerResponse response, final String file, final String mimeType) {
        response.reset();

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                Resources.asByteSource(Resources.getResource(DatasetView.class, file)).copyTo(output);
            }
        };

        Response r = Response.ok(stream, mimeType).build();

        response.setResponse(r);
    }
}
