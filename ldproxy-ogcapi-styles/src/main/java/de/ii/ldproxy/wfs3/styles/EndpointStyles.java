/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import de.ii.ldproxy.ogcapi.application.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * fetch list of styles or a style for the service
 */
@Component
@Provides
@Instantiate
public class EndpointStyles implements OgcApiEndpointExtension, ConformanceClass, StylesFormatExtension {

    // TODO change to query handler approach

    @Requires
    I18n i18n;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStyles.class);

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("styles")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^/?(?:\\w+(?:/metadata)?)?$")
            .build();

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
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
                            .build(),
                    new ImmutableOgcApiMediaType.Builder()
                            .type(MediaType.TEXT_HTML_TYPE)
                            .build()
                    );
        else if (subPath.matches("^/?\\w+$"))
            return getStyleFormatStream(dataset).map(StyleFormatExtension::getMediaType)
                    .collect(ImmutableSet.toImmutableSet());

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }

    private Stream<StyleFormatExtension> getStyleFormatStream(OgcApiDatasetData apiData) {
        return extensionRegistry.getExtensionsForType(StyleFormatExtension.class)
                .stream()
                .filter(styleFormatExtension -> styleFormatExtension.isEnabledForApi(apiData));
    }

    private List<OgcApiMediaType> getMediaTypes(OgcApiDatasetData apiData, File apiDir, String styleId) {
        return getStyleFormatStream(apiData)
                .filter(styleFormat -> new File(apiDir + File.separator + styleId + "." + styleFormat.getFileExtension()).exists())
                .map(StyleFormatExtension::getMediaType)
                .collect(Collectors.toList());
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, StylesConfiguration.class);
    }

    /**
     * fetch all available styles for the service
     *
     * @return all styles in a JSON styles object or an HTML page
     */
    @Path("/")
    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.TEXT_HTML})
    public Response getStyles(@Context OgcApiDataset api, @Context OgcApiRequestContext requestContext) {
        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

        final String apiId = api.getId();
        File apiDir = new File(stylesStore + File.separator + apiId);
        if (!apiDir.exists()) {
            apiDir.mkdirs();
        }

        Styles styles = ImmutableStyles.builder()
                .styles(
                        Arrays.stream(apiDir.listFiles())
                            .filter(file -> !file.isHidden())
                            .map(File::getName)
                            .sorted()
                            .filter(filename -> Files.getFileExtension(filename).equalsIgnoreCase("metadata"))
                            .map(filename -> ImmutableStyleEntry.builder()
                                    .id(Files.getNameWithoutExtension(filename))
                                    .title(getMetadata(Files.getNameWithoutExtension(filename), requestContext).get().getTitle())
                                    .links(stylesLinkGenerator.generateStyleLinks(requestContext.getUriCustomizer(),
                                                                         Files.getNameWithoutExtension(filename),
                                                                         getMediaTypes(api.getData(),
                                                                                 apiDir,
                                                                                 Files.getNameWithoutExtension(filename)),
                                                                         i18n,
                                                                         requestContext.getLanguage()))
                                    .build())
                            .collect(Collectors.toList()))
                .links(new DefaultLinksGenerator()
                        .generateLinks(requestContext.getUriCustomizer(),
                                requestContext.getMediaType(),
                                requestContext.getAlternateMediaTypes(),
                                i18n,
                                requestContext.getLanguage()))
                .build();

        if (requestContext.getMediaType().matches(MediaType.TEXT_HTML_TYPE)) {
            Optional<StylesFormatExtension> outputFormatHtml = api.getOutputFormat(StylesFormatExtension.class, requestContext.getMediaType(), "/styles");
            if (outputFormatHtml.isPresent())
                return outputFormatHtml.get().getStylesResponse(styles, api, requestContext);

            throw new NotAcceptableException();
        }

        return getStylesResponse(styles, api, requestContext);
    }

    @Override
    public Response getStylesResponse(Styles styles, OgcApiDataset api, OgcApiRequestContext requestContext) {
        return Response.ok(styles)
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
    public Response getStyleMetadata(@PathParam("styleId") String styleId,
                                     @Context OgcApiDataset api,
                                     @Context OgcApiRequestContext requestContext) {

        StyleMetadata metadata = getMetadata(styleId, requestContext).orElseThrow(InternalServerErrorException::new);

        if (requestContext.getMediaType().matches(MediaType.TEXT_HTML_TYPE)) {
            Optional<StylesFormatExtension> outputFormatHtml = api.getOutputFormat(StylesFormatExtension.class, requestContext.getMediaType(), "/styles");
            if (outputFormatHtml.isPresent())
                return outputFormatHtml.get().getStyleMetadataResponse(metadata, api, requestContext);

            throw new NotAcceptableException();
        }

        return getStyleMetadataResponse(metadata, requestContext.getApi(), requestContext);
    }

    @Override
    public Response getStyleMetadataResponse(StyleMetadata metadata, OgcApiDataset api, OgcApiRequestContext requestContext) {
        return Response.ok()
                .entity(metadata)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    private Optional<StyleMetadata> getMetadata(String styleId, OgcApiRequestContext requestContext) {
        String key = styleId + ".metadata";
        String apiId = requestContext.getApi().getId();
        File metadataFile = new File( stylesStore + File.separator + apiId + File.separator + styleId + ".metadata");

        if (!metadataFile.exists()) {
            throw new NotFoundException();
        }

        try {
            final byte[] metadataContent = java.nio.file.Files.readAllBytes(metadataFile.toPath());

            // prepare Jackson mapper for deserialization
            final ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new Jdk8Module());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                // parse input
                StyleMetadata metadata = mapper.readValue(metadataContent, StyleMetadata.class);

                // TODO add standard links to preview?
                return Optional.of(metadata);
            } catch (IOException e) {
                LOGGER.error("Style metadata file in styles store is invalid: "+metadataFile.getAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.error("Style metadata could not be read: "+styleId);
        }
        return Optional.empty();
    };
}
