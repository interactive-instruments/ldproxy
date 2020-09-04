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
import com.google.common.io.Files;
import de.ii.ldproxy.ogcapi.domain.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.styles.domain.*;
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
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.ii.ldproxy.ogcapi.domain.FoundationConfiguration.API_RESOURCES_DIR;
import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;

/**
 * fetch list of styles or metadata for a style
 */
@Component
@Provides
@Instantiate
public class EndpointStyles extends Endpoint implements ConformanceClass {

    @Requires
    I18n i18n;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStyles.class);

    private static final List<String> TAGS = ImmutableList.of("Discover and fetch styles");

    private final java.nio.file.Path stylesStore;

    public EndpointStyles(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                          @Requires ExtensionRegistry extensionRegistry) throws IOException {
        super(extensionRegistry);
        this.stylesStore = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), API_RESOURCES_DIR)
                                .resolve("styles");
        if (java.nio.file.Files.notExists(stylesStore)) {
            if (java.nio.file.Files.notExists(stylesStore.getParent())) {
                java.nio.file.Files.createDirectory(stylesStore.getParent());
            }
            java.nio.file.Files.createDirectory(stylesStore);
        }
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-styles-1/0.0/conf/core");
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(StylesFormatExtension.class);
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
                    .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_STYLES);
            List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, "/styles");
            String operationSummary = "lists the available styles";
            Optional<String> operationDescription = Optional.of("This operation fetches the set of styles available. " +
                    "For each style the id, a title, links to the stylesheet of the style in each supported encoding, " +
                    "and the link to the metadata is provided.");
            String path = "/styles";
            ImmutableOgcApiResourceSet.Builder resourceBuilderSet = new ImmutableOgcApiResourceSet.Builder()
                    .path(path)
                    .subResourceType("Style");
            ApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilderSet.putOperations("GET", operation);
            definitionBuilder.putResources(path, resourceBuilderSet.build());

            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
    }

    /**
     * fetch all available styles for the service
     *
     * @return all styles in a JSON styles object or an HTML page
     */
    @Path("/")
    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.TEXT_HTML})
    public Response getStyles(@Context OgcApi api, @Context ApiRequestContext requestContext) {
        final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

        final String apiId = api.getId();
        File apiDir = new File(stylesStore + File.separator + apiId);
        if (!apiDir.exists()) {
            apiDir.mkdirs();
        }

        Optional<StylesConfiguration> stylesExtension = api.getData().getExtension(StylesConfiguration.class);
        Styles styles = ImmutableStyles.builder()
                                       .styles(
                        Arrays.stream(apiDir.listFiles())
                              .filter(file -> !file.isHidden())
                              .map(File::getName)
                              .filter(filename -> Files.getFileExtension(filename).equalsIgnoreCase("metadata"))
                              .sorted()
                              .map(filename -> ImmutableStyleEntry.builder()
                                    .id(Files.getNameWithoutExtension(filename))
                                    .title(getMetadata(Files.getNameWithoutExtension(filename), requestContext).get().getTitle())
                                    .links(stylesLinkGenerator.generateStyleLinks(requestContext.getUriCustomizer(),
                                                                         Files.getNameWithoutExtension(filename),
                                                                         getStylesheetMediaTypes(api.getData(),
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

        return getFormats().stream()
                .filter(format -> requestContext.getMediaType().matches(format.getMediaType().type()))
                .findAny()
                .map(StylesFormatExtension.class::cast)
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated.", requestContext.getMediaType().type())))
                .getStylesResponse(styles, api, requestContext);
    }

    private Optional<StyleMetadata> getMetadata(String styleId, ApiRequestContext requestContext) {
        String key = styleId + ".metadata";
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
                return Optional.of(metadata);
            } catch (IOException e) {
                LOGGER.error("Cannot determine style title. Style metadata file in styles store is invalid: "+metadataFile.getAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.error("Cannot determine style title. Style metadata could not be read: "+styleId);
        }
        return Optional.empty();
    }

    private List<ApiMediaType> getStylesheetMediaTypes(OgcApiDataV2 apiData, File apiDir, String styleId) {
        return extensionRegistry.getExtensionsForType(StyleFormatExtension.class)
                .stream()
                .sorted(Comparator.comparing(StyleFormatExtension::getFileExtension))
                .filter(styleFormatExtension -> styleFormatExtension.isEnabledForApi(apiData))
                .filter(styleFormat -> new File(apiDir + File.separator + styleId + "." + styleFormat.getFileExtension()).exists())
                .map(StyleFormatExtension::getMediaType)
                .collect(Collectors.toList());
    }
}
