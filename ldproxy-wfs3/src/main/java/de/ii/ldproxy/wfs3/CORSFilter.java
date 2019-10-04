/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class CORSFilter implements ContainerResponseFilter {

    // TODO review

    private static final Logger LOGGER = LoggerFactory.getLogger(CORSFilter.class);

    //@Context
    //private Wfs3RequestContext wfs3Request;

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        if (!requestContext.getUriInfo()
                           .getPath()
                           .startsWith("admin")) {
            //Wfs3MediaType mediaType = wfs3Request.getMediaType();
            //if (mediaType.matches(MediaType.APPLICATION_JSON_TYPE) || mediaType.matches(new MediaType("application", "vnd.mapbox-vector-tile"))) {
            //TODO
            if (requestContext.getAcceptableMediaTypes()
                              .get(0)
                              .isCompatible(MediaType.APPLICATION_JSON_TYPE)
                    || requestContext.getAcceptableMediaTypes()
                                     .get(0)
                                     .isCompatible(new MediaType("application", "vnd.mapbox-vector-tile"))) {
                responseContext.getHeaders()
                               .add(
                                       "Access-Control-Allow-Origin", "*");
                responseContext.getHeaders()
                               .add(
                                       "Access-Control-Allow-Credentials", "true");
                responseContext.getHeaders()
                               .add(
                                       "Access-Control-Allow-Headers",
                                       "origin, content-type, accept, authorization");
                responseContext.getHeaders()
                               .add(
                                       "Access-Control-Allow-Methods",
                                       "GET, OPTIONS, HEAD");
            }
        }
    }
}
