/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext.HttpMethods;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

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
            .subPathPattern("^/?(?:\\w+(?:/metadata)?)?$")
            .build();

    private final File stylesStore;
    private final OgcApiExtensionRegistry extensionRegistry;

    public EndpointStyles(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext, @Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
        this.stylesStore = new File(bundleContext.getProperty(DATA_DIR_KEY) + File.separator + "styles");
        if (!stylesStore.exists()) {
            stylesStore.mkdirs();
        }
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
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData dataset, String subPath) {
        if (subPath.matches("^/?$|^/?\\w+/metadata$"))
            return ImmutableSet.of(
                    new ImmutableOgcApiMediaType.Builder()
                            .type(MediaType.APPLICATION_JSON_TYPE)
                            .build());
        else if (subPath.matches("^/?\\w+$"))
            return getStyleFormatStream(dataset).map(StyleFormatExtension::getMediaType)
                    .collect(ImmutableSet.toImmutableSet());

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }

    private Stream<StyleFormatExtension> getStyleFormatStream(OgcApiDatasetData dataset) {
        return extensionRegistry.getExtensionsForType(StyleFormatExtension.class)
                                .stream()
                                .filter(styleFormatExtension -> styleFormatExtension.isEnabledForApi(dataset));
    }

    private List<OgcApiMediaType> getMediaTypes(OgcApiDatasetData apiData, File apiDir, String styleId) {
        return getStyleFormatStream(apiData)
                .filter(styleFormat -> new File( apiDir + File.separator + styleId + "." + styleFormat.getFileExtension()).exists())
                .map(StyleFormatExtension::getMediaType)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, StylesConfiguration.class);
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
        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

        final String datasetId = dataset.getId();
        File apiDir = new File(stylesStore + File.separator + datasetId);
        if (!apiDir.exists()) {
            apiDir.mkdirs();
        }

        List<Map<String, Object>> styles = Arrays.stream(apiDir.listFiles())
                .filter(file -> !file.isHidden())
                .map(File::getName)
                .sorted()
                .filter(filename -> Files.getFileExtension(filename).equalsIgnoreCase("metadata"))
                .map(filename -> ImmutableMap.<String, Object>builder()
                        .put("id", Files.getNameWithoutExtension(filename))
                        .put("links", stylesLinkGenerator.generateStyleLinks(ogcApiRequest.getUriCustomizer(),
                                                                             Files.getNameWithoutExtension(filename),
                                                                             getMediaTypes(dataset.getData(),
                                                                                     apiDir,
                                                                                     Files.getNameWithoutExtension(filename))))
                        .build())
                .collect(Collectors.toList());

        if (styles.size() == 0) {
            return Response.ok("{\"styles\":[]}")
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }

        return Response.ok(ImmutableMap.of("styles", styles))
                .type(MediaType.APPLICATION_JSON_TYPE)
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
                                                                                                                                .type()))
                                                                                  .findFirst()
                                                                                  .orElseThrow(NotAcceptableException::new);

        String key = styleId + "." + styleFormat.getFileExtension();
        String datasetId = dataset.getId();
        File stylesheet = new File( stylesStore + File.separator + datasetId + File.separator + styleId + "." + styleFormat.getFileExtension());
        File metadata = new File( stylesStore + File.separator + datasetId + File.separator + styleId + ".metadata");
        if (!stylesheet.exists()) {
            if (metadata.exists()) {
                throw new NotAcceptableException();
            } else {
                throw new NotFoundException();
            }
        }

        try {
            final byte[] style = java.nio.file.Files.readAllBytes(stylesheet.toPath());

            return Response.ok()
                           .entity(style)
                           .type(styleFormat.getMediaType()
                                            .type())
                           .build();
        } catch (IOException e) {
            throw new ServerErrorException("stylesheet could not be read: "+styleId, 500);
        }
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
        File metadataFile = new File( stylesStore + File.separator + datasetId + File.separator + styleId + ".metadata");

        if (!metadataFile.exists()) {
            throw new NotFoundException();
        }

        try {
            final byte[] metadata = java.nio.file.Files.readAllBytes(metadataFile.toPath());

            return Response.ok()
                    .entity(metadata)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        } catch (IOException e) {
            throw new ServerErrorException("stylesheet could not be read: "+styleId, 500);
        }
    }
}
