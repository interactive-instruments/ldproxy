/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.infra.rest.Wfs3RequestContextBinder;
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
import java.util.List;
import java.util.Objects;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class CORSFilter implements ContainerResponseFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CORSFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        if (!requestContext.getUriInfo()
                           .getPath()
                           .startsWith("admin")) {
            boolean corsEnabled = false;

            Object ogcApiRequestContext = requestContext.getProperty(Wfs3RequestContextBinder.WFS3_REQUEST_CONTEXT_KEY);
            if (Objects.nonNull(ogcApiRequestContext) && ogcApiRequestContext instanceof OgcApiRequestContext) {
                OgcApiRequestContext context = (OgcApiRequestContext) ogcApiRequestContext;
                corsEnabled = context.getMediaType().matches(MediaType.APPLICATION_JSON_TYPE) || context.getMediaType().matches(new MediaType("application", "vnd.mapbox-vector-tile"));

                if (!corsEnabled && !context.getAlternativeMediaTypes().isEmpty()) {
                    corsEnabled = context.getAlternativeMediaTypes().stream().anyMatch(mediaType -> mediaType.matches(MediaType.APPLICATION_JSON_TYPE) || mediaType.matches(new MediaType("application", "vnd.mapbox-vector-tile")));
                }
            } else {
                MediaType mediaType = requestContext.getAcceptableMediaTypes()
                                                    .get(0);
                corsEnabled = mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE) || mediaType.isCompatible(new MediaType("application", "vnd.mapbox-vector-tile"));
            }

            if (corsEnabled) {
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
