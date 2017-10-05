/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
