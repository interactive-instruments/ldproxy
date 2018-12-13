
/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.api.*;
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
import java.io.*;
import java.util.*;

/**
 * fetch list of styles or a style for a collection
 *
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointStylesCollection implements Wfs3EndpointExtension {

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

    /**
     * retrieve all available styles of a specific collection with metadata and links to them
     *
     * @param collectionId  the id of the collection you want to get all styles
     * @return all styles for the collection in a json array
     */
    @Path("/{collectionId}/styles")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStylesCollection(@PathParam("collectionId") String collectionId, @Context Service service, @Context Wfs3RequestContext wfs3Request) throws IOException, KeyNotFoundException {

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles").getChildStore(service.getId()).getChildStore(collectionId);
        List<String> keys = stylesStore.getKeys();

        List<Map<String,Object>> styles =new ArrayList<>();
        for(String key: keys){
            if(stylesStore.containsKey(key)){
                Map<String, Object> styleJson = Wfs3EndpointStyles.getStyleJson(stylesStore,key);

                if(styleJson!=null){
                    Map<String,Object> styleInfo=new HashMap<>();
                    final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();
                    String styleId = key.split("\\.")[0];
                    styleInfo.put("identifier", styleId);
                    styleInfo.put("links",stylesLinkGenerator.generateStylesLinksCollection(wfs3Request.getUriCustomizer(), styleId));
                    styles.add(styleInfo);
                }

            }
        }
        if(styles.size()==0){
            throw new NotFoundException();
        }


        return Response.ok(ImmutableMap.of("styles", styles)).build();
    }

    /**
     * retrieve one specific style of the collection by id
     *
     * @param collectionId  the id of the collection you want to get a style from
     * @param styleId the local identifier of a specific style
     * @return the style in a json file
     */
    @Path("/{collectionId}/styles/{styleId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStyle(@PathParam("collectionId") String collectionId, @PathParam("styleId") String styleId, @Context Service service) throws IOException, KeyNotFoundException {

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles").getChildStore(service.getId()).getChildStore(collectionId);
        List<String> styles = stylesStore.getKeys();

        Map<String, Object> styleToDisplay = Wfs3EndpointStyles.getStyleToDisplay(stylesStore,styles,styleId);

        return Response.ok(styleToDisplay).build();
    }




}
