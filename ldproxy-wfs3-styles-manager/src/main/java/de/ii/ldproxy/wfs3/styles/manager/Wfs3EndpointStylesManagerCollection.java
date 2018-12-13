package de.ii.ldproxy.wfs3.styles.manager;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.service.api.Service;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * creates, updates and deletes a style from the collection
 *
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointStylesManagerCollection implements Wfs3EndpointExtension {

    @Requires
    private KeyValueStore keyValueStore;

    @Override
    public String getPath() {
        return "collections";
    }

    @Override
    public String getSubPathRegex() {
        return "^\\/(?:\\w+)\\/styles\\/?.*$";
    }

    @Override
    public List<String> getMethods() {
        return ImmutableList.of("PUT", "DELETE");
    }

    /**
     * updates one specific style of the collection
     *
     * @param styleId      the local identifier of a specific style
     * @param collectionId the id of the collection you want to get a style from
     * @return
     */
    @Path("/{collectionId}/styles/{styleId}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putStyleCollection(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId, @PathParam("styleId") String styleId, @Context Service service, @Context Wfs3RequestContext wfs3Request, @Context HttpServletRequest request, InputStream requestBody){

        checkAuthorization(((Wfs3Service) service).getData(), optionalUser);

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles").getChildStore(service.getId()).getChildStore(collectionId);

        List<String> styles = stylesStore.getKeys();

        Scanner s = new Scanner(requestBody).useDelimiter("\\A");
        String requestBodyString = s.hasNext() ? s.next() : ""; //TODO format string with \n

        if(!Wfs3EndpointStylesManager.validateRequestBody(requestBodyString))
            throw new BadRequestException();

        Wfs3EndpointStylesManager.putProcess(stylesStore,styles,styleId,requestBodyString);

        return Response.noContent().build();
    }


    /**
     * deletes one specific style of the collection
     *
     * @param styleId      the local identifier of a specific style
     * @param collectionId the id of the collection you want to get a style from
     * @return
     */
    @Path("/{collectionId}/styles/{styleId}")
    @DELETE
    public Response deleteStyleCollection(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId, @PathParam("styleId") String styleId, @Context Service service){

        checkAuthorization(((Wfs3Service) service).getData(), optionalUser);

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles").getChildStore(service.getId()).getChildStore(collectionId);
        List<String> styles = stylesStore.getKeys();

        Wfs3EndpointStylesManager.deleteProcess(stylesStore, styles, styleId);

        return Response.noContent().build();
    }

}
