/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadata;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadataFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
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
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;

/**
 * fetch list the metadata of a style
 */
@Component
@Provides
@Instantiate
public class EndpointStyleMetadata extends Endpoint {

    @Requires
    I18n i18n;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStyleMetadata.class);

    private static final List<String> TAGS = ImmutableList.of("Discover and fetch styles");

    private final File stylesStore;

    public EndpointStyleMetadata(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                                 @Requires ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
        this.stylesStore = new File(bundleContext.getProperty(DATA_DIR_KEY) + File.separator + "styles");
        if (!stylesStore.exists()) {
            stylesStore.mkdirs();
        }
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(StyleMetadataFormatExtension.class);
        return formats;
    }

    @Override
    public ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                    .apiEntrypoint("styles")
                    .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_STYLE_METADATA);
            String path = "/styles/{styleId}/metadata";
            ImmutableList<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
            List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
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
                ApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
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
                                     @Context OgcApi api,
                                     @Context ApiRequestContext requestContext) {

        StyleMetadata metadata = getMetadata(api, styleId, requestContext);

        return getFormats().stream()
                .filter(format -> requestContext.getMediaType().matches(format.getMediaType().type()))
                .findAny()
                .map(StyleMetadataFormatExtension.class::cast)
                .orElseThrow(() -> new NotAcceptableException(
                        MessageFormat.format("The requested media type ''{0}'' is not supported, the following media types are available: {1}",
                                requestContext.getMediaType(),
                                String.join(", ",getFormats().stream().map(f -> f.getMediaType().type().toString()).collect(Collectors.toList())))))
                .getStyleMetadataResponse(metadata, api, requestContext);
    }

    private StyleMetadata getMetadata(OgcApi api, String styleId, ApiRequestContext requestContext) {
        String apiId = requestContext.getApi().getId();
        File metadataFile = new File( stylesStore + File.separator + apiId + File.separator + styleId + ".metadata");

        if (!metadataFile.exists()) {
            throw new NotFoundException(MessageFormat.format("The style ''{0}'' does not exist in this API.", styleId));
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
                return metadata;
            } catch (IOException e) {
                LOGGER.error("Style metadata file in styles store is invalid: "+metadataFile.getAbsolutePath());
                throw new InternalServerErrorException(MessageFormat.format("Style metadata file in styles store is invalid for style ''{0}'' in API ''{1}''.", styleId, api.getId()), e);
            }
        } catch (IOException e) {
            LOGGER.error("Style metadata could not be read: "+styleId);
            throw new InternalServerErrorException(MessageFormat.format("Style metadata could not be read for style ''{0}'' in API ''{1}''.", styleId, api.getId()), e);
        }
    }
}
