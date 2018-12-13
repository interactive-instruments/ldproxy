package de.ii.ldproxy.wfs3.styles.manager;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.xsf.configstore.api.KeyValueStore;
import de.ii.xsf.configstore.api.Transaction;
import de.ii.xsf.configstore.api.WriteTransaction;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * creates, updates and deletes a style from the service
 *
 */
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
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putStyle(@Auth Optional<User> optionalUser, @PathParam("styleId") String styleId, @Context Service service, @Context Wfs3RequestContext wfs3Request, @Context HttpServletRequest request, InputStream requestBody) {

        checkAuthorization(((Wfs3Service) service).getData(), optionalUser);

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles").getChildStore(service.getId());

        List<String> styles = stylesStore.getKeys();

        Scanner s = new Scanner(requestBody).useDelimiter("\\A");
        String requestBodyString = s.hasNext() ? s.next() : "";

        if(!validateRequestBody(requestBodyString))
            throw new BadRequestException();

        putProcess(stylesStore,styles,styleId,requestBodyString);

        return Response.noContent().build();
    }


    /**
     * deletes one specific style of the dataset
     *
     * @param styleId the local identifier of a specific style
     * @return
     */
    @Path("/{styleId}")
    @DELETE
    public Response deleteStyle(@Auth Optional<User> optionalUser, @PathParam("styleId") String styleId, @Context Service service){

        checkAuthorization(((Wfs3Service) service).getData(), optionalUser);

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles").getChildStore(service.getId());
        List<String> styles = stylesStore.getKeys();

        deleteProcess(stylesStore,styles,styleId);

        return Response.noContent().build();
    }

    /**
     * search for the style in the store and delete it
     *
     * @param stylesStore   the key value store
     * @param styles        a list of all available Styles
     * @param styleId       the id of the style, that should be deleted
     */
    public static void deleteProcess(KeyValueStore stylesStore,List<String> styles,String styleId){
        boolean styleFound=false;
        for(String style: styles){
            if(style.split("\\.")[0].equals(styleId)){
                styleFound=true;
                Transaction deleteTransaction = stylesStore.openDeleteTransaction(style);
                try {
                    deleteTransaction.execute(); //TODO should throw exception
                    deleteTransaction.commit();
                }
                catch(IOException e){
                    deleteTransaction.rollback();
                }
                finally {
                 deleteTransaction.close();
                }
            }
            //TODO workaround if delete process not successful
            if(stylesStore.containsKey(style)){
                throw new InternalError();

            }
        }
        if(!styleFound){
            throw new NotFoundException();
        }
    }

    /**
     * search for the style in the store and update it, or create a new one
     *
     * @param stylesStore   the key value store
     * @param styles        a list of all available Styles
     * @param styleId       the id of the style, that should be updated or created
     * @param requestBodyString   the new Style as a String
     */
    public static void putProcess(KeyValueStore stylesStore, List<String> styles, String styleId,String requestBodyString){
        boolean styleFound=false;
        for(String style: styles){
            try {
                if(style.split("\\.")[0].equals(styleId)){
                    WriteTransaction<String> transaction = stylesStore.openWriteTransaction(style);
                    putTransaction(transaction,requestBodyString);
                    styleFound=true;
                }
            }catch(NullPointerException ignored){ }

        }
        if(!styleFound){
            WriteTransaction<String> transaction = stylesStore.openWriteTransaction(styleId+".json");
            putTransaction(transaction,requestBodyString);
        }
    }

    /**
     * a complete put transaction
     *
     * @param transaction    the write Transaction on the specific Style
     * @param requestBodyString    the new Style as a String
     */
    public static void putTransaction(WriteTransaction<String> transaction, String requestBodyString){
        try {
            transaction.write(requestBodyString);
            transaction.execute();
            transaction.commit();
        }catch(IOException e){
            transaction.rollback();
        }
        finally {
            transaction.close();
        }
    }

    /**
     * checks if the request body from the PUT-Request is valid
     *
     * @param requestBodyString     the new Style as a String
     * @return true if json and content is valid
     */
    public static boolean validateRequestBody(String requestBodyString){

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
        try {
            objectMapper.readTree(requestBodyString);
        } catch (Exception e) {
            return false;
        }
        //TODO check content when specification is clear


        return true;
    }
}
