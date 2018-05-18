/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.io.Resources;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.net.URL;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class WfsTargetHtml {

    @Context
    private BundleContext bc;

    public Response getFile(String file) {
        try {
            final URL url = file.endsWith("favicon.ico") ? bc.getBundle().getResource("img/favicon.ico") : bc.getBundle().getResource(file);

            MediaType mediaType = file.endsWith(".css") ? new MediaType("text", "css", "utf-8") : file.endsWith(".js") ? new MediaType("application", "javascript", "utf-8") : new MediaType("image", "x-icon");

            return Response.ok((StreamingOutput) output -> Resources.asByteSource(url).copyTo(output)).type(mediaType).build();
        } catch (Exception e) {
            throw new NotFoundException();
        }
    }
}
