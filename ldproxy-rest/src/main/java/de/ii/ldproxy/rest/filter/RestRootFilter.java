/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
