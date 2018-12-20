package de.ii.ldproxy.wfs3.styles.representation;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xtraplatform.service.api.Service;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Component
@Provides
@Instantiate
public class Wfs3EndpointStylesRepresentationCollection implements Wfs3EndpointExtension {

    @Requires
    private KeyValueStore keyValueStore;

    @Override
    public String getPath() {
        return "collections";
    }

    @Override
    public String getSubPathRegex() {
        return "^\\/?.*$";
    }

    @Override
    public List<String> getMethods() {
        return ImmutableList.of("GET");
    }

    @Override
    public boolean matches(String firstPathSegment, String method, String subPath) {
        return Wfs3EndpointExtension.super.matches(firstPathSegment, method, subPath);
    }

    @Path("/{collectionId}/maps/{styleId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStylesRepresentationCollection(@PathParam("collectionId") String collectionId,@PathParam("styleId") String styleId, @Context Service service, @Context Wfs3RequestContext wfs3Request){


        return Response.ok().build();
    }

}
