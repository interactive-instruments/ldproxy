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
package de.ii.ldproxy.rest;

import com.google.common.io.Resources;
import de.ii.xsf.core.api.MediaTypeCharset;
import de.ii.xsf.logging.XSFLogger;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.xml.sax.InputSource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author zahnen
 */

// TODO: move to core rest and make generic, generate json from config

@Component
@Provides(specifications = {SwaggerApiResource.class})
@Instantiate
/*@Wbp(
        filter = "(objectClass=de.ii.xsf.core.api.rest.ServiceResourceFactory)",
        onArrival = "onServiceResourceArrival",
        onDeparture = "onServiceResourceDeparture")
*/
@Path("/api/")
@Produces(MediaTypeCharset.APPLICATION_JSON_UTF8)
public class SwaggerApiResource {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(SwaggerApiResource.class);

    @GET
    public Response getApi() {
        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                Resources.asByteSource(Resources.getResource(SwaggerApiResource.class, "/swagger/swagger.json")).copyTo(output);
            }
        };

        return Response.ok(stream).build();
    }
}
