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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * fetch list the metadata of a style
 */
@Component
@Provides
@Instantiate
public class EndpointStyleMetadata extends OgcApiEndpoint {

    @Requires
    I18n i18n;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStyleMetadata.class);

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("styles")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^/?[^/]+/metadata/?$")
            .build();

    private static final List<String> TAGS = ImmutableList.of("Discover and fetch styles");

    private final File stylesStore;

    public EndpointStyleMetadata(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                                 @Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
        this.stylesStore = new File(bundleContext.getProperty(DATA_DIR_KEY) + File.separator + "styles");
        if (!stylesStore.exists()) {
            stylesStore.mkdirs();
        }
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    private List<OgcApiMediaType> getStylesheetMediaTypes(OgcApiApiDataV2 apiData, File apiDir, String styleId) {
        return extensionRegistry.getExtensionsForType(StyleFormatExtension.class)
                .stream()
                .filter(styleFormatExtension -> styleFormatExtension.isEnabledForApi(apiData))
                .filter(styleFormat -> new File(apiDir + File.separator + styleId + "." + styleFormat.getFileExtension()).exists())
                .map(StyleFormatExtension::getMediaType)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, StylesConfiguration.class);
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(StyleMetadataFormatExtension.class);
        return formats;
    }

    @Override
    public OgcApiEndpointDefinition getDefinition(OgcApiApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableOgcApiEndpointDefinition.Builder definitionBuilder = new ImmutableOgcApiEndpointDefinition.Builder()
                    .apiEntrypoint("styles")
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_STYLE_METADATA);
            String path = "/styles/{styleId}/metadata";
            ImmutableSet<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
            Set<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
            if (!pathParameters.stream().filter(param -> param.getName().equals("styleId")).findAny().isPresent()) {
                LOGGER.error("Path parameter 'styleId' missing for resource at path '" + path + "'. The GET method will not be available.");
            } else {
                String operationSummary = "fetch metadata about the style `{styleId}`";
                Optional<String> operationDescription = Optional.of("Style metadata is essential information about a style in order to " +
                        "support users to discover and select styles for rendering their data and for visual style editors " +
                        "to create user interfaces for editing a style. This operations returns the metadata for the " +
                        "requested style as a single document. The stylesheet of the style will typically include some " +
                        "the metadata, too.");
                ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                        .path(path)
                        .pathParameters(pathParameters);
                OgcApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
                if (operation!=null)
                    resourceBuilder.putOperations("GET", operation);
                definitionBuilder.putResources(path, resourceBuilder.build());
            }

            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
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

        return getFormats().stream()
                .filter(format -> requestContext.getMediaType().matches(format.getMediaType().type()))
                .findAny()
                .map(StyleMetadataFormatExtension.class::cast)
                .orElseThrow(() -> new NotAcceptableException())
                .getStyleMetadataResponse(metadata, api, requestContext);
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
