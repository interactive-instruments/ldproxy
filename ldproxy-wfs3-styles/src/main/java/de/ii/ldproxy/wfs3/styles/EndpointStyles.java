/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiEndpointExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.GET;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * fetch list of styles or a style for the service
 */
@Component
@Provides
@Instantiate
public class EndpointStyles implements OgcApiEndpointExtension, ConformanceClass {

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("styles")
            .addMethods(HttpMethods.GET)
            .build();

    private final StylesStore stylesStore;
    private final OgcApiExtensionRegistry extensionRegistry;

    public EndpointStyles(@Requires StylesStore stylesStore, @Requires OgcApiExtensionRegistry extensionRegistry) {
        this.stylesStore = stylesStore;
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/t15/opf-styles-1/1.0/conf/core";
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData dataset) {
        return getStyleFormatStream(dataset).map(StyleFormatExtension::getMediaType)
                                            .collect(ImmutableSet.toImmutableSet());
    }

    private Stream<StyleFormatExtension> getStyleFormatStream(OgcApiDatasetData dataset) {
        return extensionRegistry.getExtensionsForType(StyleFormatExtension.class)
                                .stream()
                                .filter(styleFormatExtension -> styleFormatExtension.isEnabledForDataset(dataset));
    }

    @Override
    public boolean isEnabledForDataset(OgcApiDatasetData datasetData) {
        return isExtensionEnabled(datasetData, StylesConfiguration.class);
    }

    /**
     * fetch all available styles for the service
     *
     * @return all styles in a JSON styles object
     */
    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStyles(@Context OgcApiDataset dataset, @Context OgcApiRequestContext ogcApiRequest) {
        List<Map<String, Object>> styles = new ArrayList<>();
        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();
        String datasetId = dataset.getId();

        for (String key : stylesStore.ids(datasetId)) {

            String styleId = Files.getNameWithoutExtension(key);
            String fileExtension = Files.getFileExtension(key);

            if (fileExtension.equalsIgnoreCase("metadata")) {
                Map<String, Object> styleInfo = new HashMap<>();
                styleInfo.put("id", styleId);
                List<OgcApiMediaType> mediaTypes = getStyleFormatStream(dataset.getData())
                        .filter(styleFormat -> stylesStore.has(styleId + "." + styleFormat.getFileExtension(), datasetId))
                        .map(StyleFormatExtension::getMediaType)
                        .collect(Collectors.toList());
                styleInfo.put("links", stylesLinkGenerator.generateStyleLinks(ogcApiRequest.getUriCustomizer(), styleId, mediaTypes));
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
    public Response getStyle(@PathParam("styleId") String styleId, @Context OgcApiDataset dataset,
                             @Context OgcApiRequestContext ogcApiRequest) {

        StyleFormatExtension styleFormat = getStyleFormatStream(dataset.getData()).filter(format -> format.getMediaType()
                                                                                                          .matches(ogcApiRequest.getMediaType()
                                                                                                                                .main()))
                                                                                  .findFirst()
                                                                                  .orElseThrow(NotAcceptableException::new);

        String key = styleId + "." + styleFormat.getFileExtension();
        String datasetId = dataset.getId();

        if (!stylesStore.has(key, datasetId)) {
            if (stylesStore.has(styleId + ".metadata", datasetId)) {
                throw new NotAcceptableException();
            } else {
                throw new NotFoundException();
            }
        }

        byte[] style = stylesStore.get(key, datasetId);

        return Response.ok()
                       .entity(style)
                       .type(styleFormat.getMediaType()
                                        .main())
                       .build();
    }

    /**
     * Fetch metadata for a style
     *
     * @param styleId the local identifier of a specific style
     * @return the style in a json file
     */
    @Path("/{styleId}/metadata")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStyleMetadata(@PathParam("styleId") String styleId, @Context OgcApiDataset dataset) {

        String key = styleId + ".metadata";
        String datasetId = dataset.getId();

        if (!stylesStore.has(key, datasetId)) {
            throw new NotFoundException();
        }

        byte[] metadata = stylesStore.get(key, datasetId);

        return Response.ok()
                       .entity(metadata)
                       .build();
    }
}
