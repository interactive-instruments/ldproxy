/**
 * Copyright 2019 interactive instruments GmbH
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
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.*;
import de.ii.xtraplatform.kvstore.api.KeyNotFoundException;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;
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

/**
 * fetch list of styles or a style for the service
 */
@Component
@Provides
@Instantiate
public class EndpointStyles implements Wfs3EndpointExtension, Wfs3ConformanceClass {

    @Requires
    private KeyValueStore keyValueStore;

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/t15/opf-styles-1/1.0/conf/core";
    }

    @Override
    public boolean isConformanceEnabledForService(Wfs3ServiceData serviceData) {
        return isEnabledForService(serviceData);
    }

    @Override
    public String getPath() {
        return "styles";
    }

    @Override
    public List<String> getMethods() {
        return ImmutableList.of("GET");
    }

    @Override
    public boolean isEnabledForService(Wfs3ServiceData serviceData) {
        return isExtensionEnabled(serviceData, StylesConfiguration.class);
    }

    /**
     * fetch all available styles for the service
     *
     * @return all styles in a JSON styles object
     */
    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStyles(@Context Service service, @Context Wfs3RequestContext wfs3Request) {
        List<Map<String, Object>> styles = new ArrayList<>();
        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                                                 .getChildStore(service.getId());

        List<String> keys = stylesStore.getKeys();

        for (String key : keys) {

            String styleId = key.substring(0,key.lastIndexOf("."));
            String fileExtension = key.substring(key.lastIndexOf(".")+1);

            if (fileExtension.equalsIgnoreCase("metadata") && stylesStore.containsKey(key)) {
                Map<String, Object> styleInfo = new HashMap<>();
                styleInfo.put("id", styleId);
                List<String> mediaTypes = new ArrayList<>();
                // TODO: Update once media type approach has been changed
                for (String key2 : stylesStore.getKeys()) {
                    if (key2.substring(0,key2.lastIndexOf(".")).equals(styleId)) {
                        String fileExtension2 = key2.substring(key2.lastIndexOf(".")+1);
                        for (Map.Entry<String,String> entry : Wfs3MediaTypes.FORMATS.entrySet()) {
                            if (entry.getValue().equals(fileExtension2))
                                mediaTypes.add(entry.getKey());
                        }
                    }
                }
                styleInfo.put("links", stylesLinkGenerator.generateStyleLinks(wfs3Request.getUriCustomizer(), styleId, mediaTypes));
                styles.add(styleInfo);
            }
        }

        if (styles.size() == 0) {
            return Response.ok("{ \n \"styles\": [] \n }")
                           .build();
        }

        return Response.ok(ImmutableMap.of("styles", styles))
                       .build();
    }

    /**
     * Fetch a style by id
     *
     * @param styleId the local identifier of a specific style
     * @return the style in a json file
     */
    @Path("/{styleId}")
    @GET
    @Produces({Wfs3MediaTypes.MBS,Wfs3MediaTypes.SLD10})
    public Response getStyle(@PathParam("styleId") String styleId, @Context Service service, @Context Wfs3RequestContext wfs3Request) {

        de.ii.ldproxy.wfs3.Wfs3Service wfs3Service = (Wfs3Service) service;
        Wfs3ServiceData serviceData = wfs3Service.getData();

        if (!isEnabledForService(serviceData))
            throw new NotFoundException();

        StyleFormatExtension ext = wfs3Service.getStyleFormatForService(wfs3Request.getMediaType(), serviceData);

        if (ext==null)
            throw new NotAcceptableException();

        return ext.getStyle(serviceData, styleId);
    }

    /**
     * Fetch metadata for a style
     *
     * @param styleId the local identifier of a specific style
     * @return the style in a json file
     */
    @Path("/{styleId}/metadata")
    @GET
    @Produces(Wfs3MediaTypes.JSON)
    public Response getStyleMetadata(@PathParam("styleId") String styleId, @Context Service service) {

        de.ii.ldproxy.wfs3.Wfs3Service wfs3Service = (Wfs3Service) service;
        Wfs3ServiceData serviceData = wfs3Service.getData();

        if (!isEnabledForService(serviceData))
            throw new NotFoundException();

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                .getChildStore(serviceData.getId());

        String key = styleId + ".metadata";

        Map<String, Object> metadata = null;

        if (stylesStore.containsKey(key)) {
            final ObjectMapper mapper = new ObjectMapper();
            try {
                metadata = mapper.readValue(stylesStore.getValueReader(key), new TypeReference<LinkedHashMap>() {
                });
            } catch (KeyNotFoundException e1) {
                throw new ServerErrorException(500); // TODO: should not occur at this point
            } catch (IOException e2) {
                throw new ServerErrorException(500); // TODO: internal error in the styles store
            }
        }

        if (metadata==null) {
            throw new NotFoundException();
        }

        return Response.ok(metadata)
                .build();
    }
    /**
     * Search the available styles for a specific style. Return a stylesheet or the style metadata
     * for the style. If no style is found, return null.
     *
     * @param stylesStore the styles store of the service
     * @param styleId     the style you want to fetch
     * @param fileExtension the document type you want to fetch (stylesheet in one of the supported encodings, the style metadata)
     * @return the style document (stylesheet or style metadata) or null, if the document is not found.
     * @throws IOException
     * @throws NotFoundException
     */
    static BufferedReader getStyleDocument(KeyValueStore stylesStore, String styleId, String fileExtension) throws IOException, KeyNotFoundException {

        BufferedReader br = null;

        String key = styleId+"."+fileExtension;
        if (stylesStore.containsKey(key)) {
            br = new BufferedReader(stylesStore.getValueReader(key));
        }
        return br;
    }
}
