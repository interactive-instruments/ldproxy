package de.ii.ldproxy.wfs3.styles.manager;


import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.xsf.configstore.api.KeyNotFoundException;
import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xtraplatform.service.api.Service;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

@Component
@Provides
@Instantiate
public class Wfs3EndpointStylesManager implements Wfs3EndpointExtension {

    @Requires
    private KeyValueStore keyValueStore;

    @Override
    public String getPath() {
        return "styles";
    }

    @Override
    public String getSubPathRegex() {
        return "^\\/?.*$";
    }
    @Override
    public List<String> getMethods() {
        return ImmutableList.of("PUT", "DELETE");
    }

    /**
     * updates one specific style of the dataset
     *
     * @param styleId the local identifier of a specific style
     * @return
     */
    @Path("/{styleId}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putStyle(@PathParam("styleId") String styleId, @Context Service service) throws IOException, KeyNotFoundException {

        //TODO

        return Response.ok().build();
    }


    /**
     * deletes one specific style of the dataset
     *
     * @param styleId the local identifier of a specific style
     * @return
     */
    @Path("/{styleId}")
    @DELETE
    public Response deleteStyle(@PathParam("styleId") String styleId, @Context Service service) throws IOException, KeyNotFoundException {

        //TODO

        return Response.ok().build();
    }
}
