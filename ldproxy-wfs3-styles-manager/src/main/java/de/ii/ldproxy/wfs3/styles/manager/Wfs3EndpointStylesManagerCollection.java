/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiContext;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiEndpointExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.wfs3.styles.StylesConfiguration;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointStylesManagerCollection implements OgcApiEndpointExtension {

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("collections")
            .subPathPattern("^\\/(?:\\w+)\\/styles\\/?.*$")
            .addMethods(HttpMethods.POST, HttpMethods.PUT, HttpMethods.DELETE)
            .build();
    private static final ImmutableSet<OgcApiMediaType> API_MEDIA_TYPES = ImmutableSet.of(
            new ImmutableOgcApiMediaType.Builder()
                    .main(MediaType.TEXT_HTML_TYPE)
                    .build()
    );

    @Requires
    private KeyValueStore keyValueStore;

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData dataset) {
        return API_MEDIA_TYPES;
    }

    @Override
    public boolean isEnabledForDataset(OgcApiDatasetData datasetData) {
        Optional<StylesConfiguration> stylesExtension = datasetData.getExtension(StylesConfiguration.class);

        return stylesExtension.isPresent() && stylesExtension.get()
                                                             .getManagerEnabled();
    }

    /**
     * creates one style for the collection
     *
     * @return
     */
    @Path("/{collectionId}/styles/")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postStyle(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId,
                              @Context OgcApiDataset service, @Context OgcApiRequestContext wfs3Request,
                              @Context HttpServletRequest request, InputStream requestBody) {

        checkAuthorization(service.getData(), optionalUser);

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
    public Response putStyleCollection(@Auth Optional<User> optionalUser,
                                       @PathParam("collectionId") String collectionId,
                                       @PathParam("styleId") String styleId, @Context OgcApiDataset service,
                                       @Context OgcApiRequestContext wfs3Request, @Context HttpServletRequest request,
                                       InputStream requestBody) {

        checkAuthorization(service.getData(), optionalUser);

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
    public Response deleteStyleCollection(@Auth Optional<User> optionalUser,
                                          @PathParam("collectionId") String collectionId,
                                          @PathParam("styleId") String styleId, @Context OgcApiDataset service) {

        checkAuthorization(service.getData(), optionalUser);

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                                                 .getChildStore(service.getId())
                                                 .getChildStore(collectionId);
        List<String> styles = stylesStore.getKeys();

        Wfs3EndpointStylesManager.deleteProcess(stylesStore, styles, styleId);

        return Response.noContent()
                       .build();
    }

}
