package de.ii.ldproxy.wfs3;

import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class CORSFilter implements ContainerResponseFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CORSFilter.class);

    @Context
    private Wfs3RequestContext wfs3Request;


    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (wfs3Request.getMediaType().matches(MediaType.APPLICATION_JSON_TYPE)) {
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
