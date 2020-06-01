/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
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
import javax.ws.rs.core.Link;
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
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/t15/opf-styles-1/1.0/conf/core");
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiApiDataV2 dataset, String subPath) {
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

    private Stream<StyleFormatExtension> getStyleFormatStream(OgcApiApiDataV2 apiData) {
        return extensionRegistry.getExtensionsForType(StyleFormatExtension.class)
                .stream()
                .filter(styleFormatExtension -> styleFormatExtension.isEnabledForApi(apiData));
    }

    private List<OgcApiMediaType> getMediaTypes(OgcApiApiDataV2 apiData, File apiDir, String styleId) {
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
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
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
    public Response getStyles(@Context OgcApiApi api, @Context OgcApiRequestContext requestContext) {
        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

        final String apiId = api.getId();
        File apiDir = new File(stylesStore + File.separator + apiId);
        if (!apiDir.exists()) {
            apiDir.mkdirs();
        }

        Optional<StylesConfiguration> stylesExtension = getExtensionConfiguration(api.getData(), StylesConfiguration.class);
        boolean maps = stylesExtension.isPresent() && stylesExtension.get()
                                                                     .getMapsEnabled();
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
                                                                         maps,
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
    public Response getStylesResponse(Styles styles, OgcApiApi api, OgcApiRequestContext requestContext) {
        boolean includeLinkHeader = getExtensionConfiguration(api.getData(), OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeLinkHeader)
                .orElse(false);

        return Response.ok(styles)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .links(includeLinkHeader ? styles.getLinks().stream().map(link -> link.getLink()).toArray(Link[]::new) : null)
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
    public Response getStyle(@PathParam("styleId") String styleId, @Context OgcApiApi dataset,
                             @Context OgcApiRequestContext ogcApiRequest) {

        StyleFormatExtension styleFormat = getStyleFormatStream(dataset.getData()).filter(format -> format.getMediaType()
                                                                                                          .matches(ogcApiRequest.getMediaType()
                                                                                                                                .type()))
                                                                                  .findFirst()
                                                                                  .orElseThrow(NotAcceptableException::new);

        MediaType mediaType = styleFormat.getMediaType().type();
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

        // collect self/alternate links, but only, if we need to return them in the headers
        List<OgcApiLink> links = null;
        boolean includeLinkHeader = getExtensionConfiguration(dataset.getData(), OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeLinkHeader)
                .orElse(false);
        if (includeLinkHeader) {
            final DefaultLinksGenerator defaultLinkGenerator = new DefaultLinksGenerator();

            final String apiId = dataset.getId();
            File apiDir = new File(stylesStore + File.separator + apiId);
            if (!apiDir.exists()) {
                apiDir.mkdirs();
            }

            List<OgcApiMediaType> alternateMediaTypes = getMediaTypes(dataset.getData(), apiDir, Files.getNameWithoutExtension(styleId)).stream()
                    .filter(availableMediaType -> !availableMediaType.matches(styleFormat.getMediaType().type()))
                    .collect(Collectors.toList());
            links = defaultLinkGenerator.generateLinks(ogcApiRequest.getUriCustomizer(), styleFormat.getMediaType(), alternateMediaTypes, i18n, ogcApiRequest.getLanguage());
        }


        try {
            final byte[] content = java.nio.file.Files.readAllBytes(stylesheet.toPath());

            if (mediaType.isCompatible(new MediaType("application","vnd.mapbox.style+json"))) {

                // prepare Jackson mapper for deserialization
                final ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new Jdk8Module());
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                try {
                    // parse input
                    MbStyleStylesheet parsedContent = mapper.readValue(content, MbStyleStylesheet.class);

                    // TODO add standard links to preview?
                    return Response.ok()
                            .entity(parsedContent)
                            .type(mediaType)
                            .links(includeLinkHeader ? links.stream().map(link -> link.getLink()).toArray(Link[]::new) : null)
                            .build();
                } catch (IOException e) {
                    LOGGER.error("Stylesheet in the styles store is invalid: " + stylesheet.getAbsolutePath());
                }
            } else if (mediaType.isCompatible(new MediaType("application","vnd.ogc.sld+xml"))) {
                // TODO

                return Response.ok()
                        .entity(content)
                        .links(includeLinkHeader ? links.stream().map(link -> link.getLink()).toArray(Link[]::new) : null)
                        .type(mediaType)
                        .build();
            }
        } catch (IOException e) {
            LOGGER.error("Stylesheet in the styles store could not be read: " + styleId);
        }

        throw new ServerErrorException("Error fetching stylesheet: "+styleId, 500);
    }

    /**
     * Fetch metadata for a style
     *
     * @param styleId the local identifier of a specific style
     * @return the style in a json file
     */
    @Path("/{styleId}/metadata")
    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.TEXT_HTML})
    public Response getStyleMetadata(@PathParam("styleId") String styleId,
                                     @Context OgcApiApi api,
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
    public Response getStyleMetadataResponse(StyleMetadata metadata, OgcApiApi api, OgcApiRequestContext requestContext) {
        boolean includeLinkHeader = getExtensionConfiguration(api.getData(), OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeLinkHeader)
                .orElse(false);

        return Response.ok()
                .entity(metadata)
                .links(includeLinkHeader ? metadata.getLinks().stream().map(link -> link.getLink()).toArray(Link[]::new) : null)
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
            mapper.registerModule(new GuavaModule());
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
    }
}
