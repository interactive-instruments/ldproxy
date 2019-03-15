/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.styles.StylesConfiguration;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;
import de.ii.xtraplatform.service.api.Service;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        return ImmutableList.of("POST", "PUT", "DELETE");
    }

    @Override
    public boolean isEnabledForService(Wfs3ServiceData serviceData) {
        Optional<StylesConfiguration> stylesExtension = serviceData.getExtension(StylesConfiguration.class);

        if (!stylesExtension.isPresent() || !stylesExtension.get()
                                                            .getManagerEnabled()) {
            throw new NotFoundException();
        }

        return true;
    }

    /**
     * creates one style for the collection
     *
     * @return
     */
    @Path("/{collectionId}/styles/")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postStyle(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId, @Context Service service, @Context Wfs3RequestContext wfs3Request, @Context HttpServletRequest request, InputStream requestBody) {

        checkAuthorization(((Wfs3Service) service).getData(), optionalUser);

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                                                 .getChildStore(service.getId())
                                                 .getChildStore(collectionId);

        List<String> styles = stylesStore.getKeys();

        Scanner s = new Scanner(requestBody).useDelimiter("\\A");
        String requestBodyString = s.hasNext() ? s.next() : "";

        JsonNode requestBodyJson = Wfs3EndpointStylesManager.validateRequestBodyJSON(requestBodyString);

        if (requestBodyJson == null || !Wfs3EndpointStylesManager.validateRequestBody(requestBodyJson))
            throw new BadRequestException();

        List<String> styleIds = new ArrayList<>();
        for (String style : styles) {
            styleIds.add(style.split("\\.")[0]);
        }

        String styleName = requestBodyJson.get("name")
                                          .asText();
        Pattern styleNamePattern = Pattern.compile("[^a-z0-9-_]", Pattern.CASE_INSENSITIVE);
        Matcher styleNameMatcher = styleNamePattern.matcher(styleName);
        if (styleIds.contains(styleName) || styleName.contains(" ") || styleNameMatcher.find()) {
            int id = 0;

            while (styleIds.contains(Integer.toString(id))) {
                id++;
            }
            Wfs3EndpointStylesManager.putProcess(stylesStore, styles, Integer.toString(id), requestBodyString);

        } else {
            Wfs3EndpointStylesManager.putProcess(stylesStore, styles, styleName, requestBodyString);
        }

        return Response.noContent()
                       .build();
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
    public Response putStyleCollection(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId, @PathParam("styleId") String styleId, @Context Service service, @Context Wfs3RequestContext wfs3Request, @Context HttpServletRequest request, InputStream requestBody) {

        checkAuthorization(((Wfs3Service) service).getData(), optionalUser);

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                                                 .getChildStore(service.getId())
                                                 .getChildStore(collectionId);

        List<String> styles = stylesStore.getKeys();

        Scanner s = new Scanner(requestBody).useDelimiter("\\A");
        String requestBodyString = s.hasNext() ? s.next() : "";

        JsonNode requestBodyJson = Wfs3EndpointStylesManager.validateRequestBodyJSON(requestBodyString);

        if (requestBodyJson == null || !Wfs3EndpointStylesManager.validateRequestBody(requestBodyJson))
            throw new BadRequestException();

        Wfs3EndpointStylesManager.putProcess(stylesStore, styles, styleId, requestBodyString);

        return Response.noContent()
                       .build();
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
    public Response deleteStyleCollection(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId, @PathParam("styleId") String styleId, @Context Service service) {

        checkAuthorization(((Wfs3Service) service).getData(), optionalUser);

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                                                 .getChildStore(service.getId())
                                                 .getChildStore(collectionId);
        List<String> styles = stylesStore.getKeys();

        Wfs3EndpointStylesManager.deleteProcess(stylesStore, styles, styleId);

        return Response.noContent()
                       .build();
    }

}
