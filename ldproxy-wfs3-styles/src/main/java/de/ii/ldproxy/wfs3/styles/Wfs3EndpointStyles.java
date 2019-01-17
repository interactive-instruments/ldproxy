package de.ii.ldproxy.wfs3.styles;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3Extension;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import static de.ii.ldproxy.wfs3.styles.StylesConfiguration.EXTENSION_KEY;

/**
 * fetch list of styles or a style for the service
 *
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointStyles implements Wfs3EndpointExtension{

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
        return ImmutableList.of("GET");
    }

    @Override
    public boolean isEnabledForService(Wfs3ServiceData serviceData){
        if(!isExtensionEnabled(serviceData,EXTENSION_KEY)){
            throw new NotFoundException();
        }
        return true;
    }
    /**
     * retrieve all available styles of the dataset with metadata and links to them
     *
     * @return all styles in a json array
     */
    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStyles(@Context Service service, @Context Wfs3RequestContext wfs3Request) throws IOException, KeyNotFoundException
    {
        List<Map<String, Object>> styles = new ArrayList<>();

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles").getChildStore(service.getId());
        List<String> keys = stylesStore.getKeys();

        for (String key : keys) {

            if (stylesStore.containsKey(key)) {

                Map<String, Object> styleJson = getStyleJson(stylesStore, key);

                if (styleJson != null) {
                    Map<String, Object> styleInfo = new HashMap<>();
                    final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();
                    String styleId = key.split("\\.")[0];

                    styleInfo.put("id", styleId);
                    styleInfo.put("links", stylesLinkGenerator.generateStylesLinksDataset(wfs3Request.getUriCustomizer(), styleId));
                    styles.add(styleInfo);
                }
            }
        }

        if (styles.size() == 0) {
            return Response.ok("{ \n \"styles\": [] \n }").build();
        }

        return Response.ok(ImmutableMap.of("styles", styles)).build();
    }

    /**
     * retrieve one specific style of the dataset by id
     *
     * @param styleId the local identifier of a specific style
     * @return the style in a json file
     */
    @Path("/{styleId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStyle( @PathParam("styleId") String styleId, @Context Service service) throws IOException, KeyNotFoundException {

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles").getChildStore(service.getId());
        List<String> styles = stylesStore.getKeys();

        Map<String, Object> styleToDisplay = getStyleToDisplay(stylesStore, styles, styleId);


        return Response.ok(styleToDisplay).build();

    }

    /**
     * converts the file into a Map and returns the style
     *
     * @param stylesStore   the styles Store of the dataset with all styles for the dataset
     * @param key           the name of one file in the collection folder
     * @return a map with the complete info of the style
     * @throws IOException
     * @throws KeyNotFoundException
     */
    public static Map<String,Object> getStyleJson(KeyValueStore stylesStore, String key)  {
        Map<String, Object> style;
        try{
            final ObjectMapper mapper = new ObjectMapper();
            BufferedReader br = new BufferedReader(stylesStore.getValueReader(key));

            if (br.readLine() == null) {
                style = null;
            } else {
                style = mapper.readValue(stylesStore.getValueReader(key), new TypeReference<LinkedHashMap>() {
                });
            }

        }catch(KeyNotFoundException|IOException e){
            throw new NotFoundException();
        }


        return style;
    }

    /**
     * search the List of available styles for the collection/dataset for a specific style. return that specific style, if no style is found returns null
     *
     * @param stylesStore   the styles Store of the dataset with all styles for the dataset
     * @param styles        a list with all the available Styles in the dataset store
     * @param styleId       the style you want to display
     * @throws IOException
     * @throws KeyNotFoundException
     */
    public static Map<String,Object> getStyleToDisplay(KeyValueStore stylesStore,List<String>styles, String styleId) {

        Map<String, Object> styleToDisplay=null;
        for(String key : styles){
            if(stylesStore.containsKey(key)){
                Map<String, Object> styleJson = Wfs3EndpointStyles.getStyleJson(stylesStore,key);
                if(styleJson!=null){
                    if(key.split("\\.")[0].equals(styleId)){
                        styleToDisplay = styleJson;
                        break;
                    }
                }
            }
        }
        if(styleToDisplay==null){
            throw new NotFoundException();
        }
        return styleToDisplay;

    }

}
