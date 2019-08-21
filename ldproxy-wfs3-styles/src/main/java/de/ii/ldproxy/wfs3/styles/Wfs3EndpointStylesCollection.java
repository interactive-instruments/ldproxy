/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.google.common.collect.ImmutableMap;
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
import de.ii.xtraplatform.kvstore.api.KeyNotFoundException;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * fetch list of styles or a style for a collection
 *
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointStylesCollection implements OgcApiEndpointExtension {

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("collections")
            .subPathPattern("^\\/?(?:\\/\\w+\\/?(?:\\/styles\\/?.*)?)?$")
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
    public boolean isEnabledForDataset(OgcApiDatasetData datasetData) {
        return isExtensionEnabled(datasetData, StylesConfiguration.class);
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
    public Response getStylesCollection(@PathParam("collectionId") String collectionId, @Context OgcApiDataset service,
                                        @Context OgcApiRequestContext wfs3Request) throws IOException, KeyNotFoundException {

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                                                 .getChildStore(service.getId())
                                                 .getChildStore(collectionId);
        List<String> keys = stylesStore.getKeys();

        List<Map<String, Object>> styles = new ArrayList<>();
        for (String key : keys) {
            if (stylesStore.containsKey(key)) {
                Map<String, Object> styleJson = Wfs3EndpointStyles.getStyleJson(stylesStore, key);

                if (styleJson != null) {
                    Map<String, Object> styleInfo = new HashMap<>();
                    final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();
                    String styleId = key.split("\\.")[0];
                    styleInfo.put("id", styleId);
                    styleInfo.put("links", stylesLinkGenerator.generateStylesLinksCollection(wfs3Request.getUriCustomizer(), styleId));
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
     * retrieve one specific style of the collection by id
     *
     * @param collectionId  the id of the collection you want to get a style from
     * @param styleId the local identifier of a specific style
     * @return the style in a json file
     */
    @Path("/{collectionId}/styles/{styleId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStyle(@PathParam("collectionId") String collectionId, @PathParam("styleId") String styleId,
                             @Context Service service) throws IOException, KeyNotFoundException {

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                                                 .getChildStore(service.getId())
                                                 .getChildStore(collectionId);
        List<String> styles = stylesStore.getKeys();

        Map<String, Object> styleToDisplay = Wfs3EndpointStyles.getStyleToDisplay(stylesStore, styles, styleId);

        return Response.ok(styleToDisplay)
                       .build();

    }


}
