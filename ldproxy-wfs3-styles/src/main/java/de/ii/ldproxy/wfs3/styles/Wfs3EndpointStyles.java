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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiContext;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiEndpointExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.xtraplatform.kvstore.api.KeyNotFoundException;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;
import de.ii.xtraplatform.service.api.Service;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * fetch list of styles or a style for the service
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointStyles implements OgcApiEndpointExtension {

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("styles")
            .addMethods(HttpMethods.GET)
            .build();
    private static final ImmutableSet<OgcApiMediaType> API_MEDIA_TYPES = ImmutableSet.of(
            new ImmutableOgcApiMediaType.Builder()
                    .main(MediaType.WILDCARD_TYPE)
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
    public boolean isEnabledForDataset(OgcApiDatasetData serviceData) {
        return isExtensionEnabled(serviceData, StylesConfiguration.class);
    }

    /**
     * retrieve all available styles of the dataset with metadata and links to them
     *
     * @return all styles in a json array
     */
    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStyles(@Context Service service,
                              @Context OgcApiRequestContext wfs3Request) throws IOException, KeyNotFoundException {
        List<Map<String, Object>> styles = new ArrayList<>();

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                                                 .getChildStore(service.getId());
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
            return Response.ok("{ \n \"styles\": [] \n }")
                           .build();
        }

        return Response.ok(ImmutableMap.of("styles", styles))
                       .build();
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
    public Response getStyle(@PathParam("styleId") String styleId,
                             @Context Service service) throws IOException, KeyNotFoundException {

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                                                 .getChildStore(service.getId());
        List<String> styles = stylesStore.getKeys();

        Map<String, Object> styleToDisplay = getStyleToDisplay(stylesStore, styles, styleId);


        return Response.ok(styleToDisplay)
                       .build();

    }

    /**
     * converts the file into a Map and returns the style
     *
     * @param stylesStore the styles Store of the dataset with all styles for the dataset
     * @param key         the name of one file in the collection folder
     * @return a map with the complete info of the style
     * @throws IOException
     * @throws KeyNotFoundException
     */
    public static Map<String, Object> getStyleJson(KeyValueStore stylesStore, String key) {
        Map<String, Object> style;
        try {
            final ObjectMapper mapper = new ObjectMapper();
            BufferedReader br = new BufferedReader(stylesStore.getValueReader(key));

            if (br.readLine() == null) {
                style = null;
            } else {
                style = mapper.readValue(stylesStore.getValueReader(key), new TypeReference<LinkedHashMap>() {
                });
            }

        } catch (KeyNotFoundException | IOException e) {
            throw new NotFoundException();
        }


        return style;
    }

    /**
     * search the List of available styles for the collection/dataset for a specific style. return that specific style, if no style is found returns null
     *
     * @param stylesStore the styles Store of the dataset with all styles for the dataset
     * @param styles      a list with all the available Styles in the dataset store
     * @param styleId     the style you want to display
     * @throws IOException
     * @throws KeyNotFoundException
     */
    public static Map<String, Object> getStyleToDisplay(KeyValueStore stylesStore, List<String> styles,
                                                        String styleId) {

        Map<String, Object> styleToDisplay = null;
        for (String key : styles) {
            if (stylesStore.containsKey(key)) {
                Map<String, Object> styleJson = Wfs3EndpointStyles.getStyleJson(stylesStore, key);
                if (styleJson != null) {
                    if (key.split("\\.")[0].equals(styleId)) {
                        styleToDisplay = styleJson;
                        break;
                    }
                }
            }
        }
        if (styleToDisplay == null) {
            throw new NotFoundException();
        }
        return styleToDisplay;

    }

}
