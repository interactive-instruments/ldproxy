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
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension;
import io.swagger.v3.oas.models.media.ObjectSchema;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * fetch list of styles or metadata for a style
 */
@Component
@Provides
@Instantiate
public class EndpointStyles extends OgcApiEndpoint implements ConformanceClass {

    @Requires
    I18n i18n;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStyles.class);

    private static final List<String> TAGS = ImmutableList.of("Discover and fetch styles");

    private final File stylesStore;

    public EndpointStyles(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                          @Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
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
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, StylesConfiguration.class);
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(StylesFormatExtension.class);
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
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_STYLES);
            List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, "/styles");
            String operationSummary = "lists the available styles";
            Optional<String> operationDescription = Optional.of("This operation fetches the set of styles available. " +
                    "For each style the id, a title, links to the stylesheet of the style in each supported encoding, " +
                    "and the link to the metadata is provided.");
            String path = "/styles";
            ImmutableOgcApiResourceSet.Builder resourceBuilderSet = new ImmutableOgcApiResourceSet.Builder()
                    .path(path)
                    .subResourceType("Style");
            OgcApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
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
    public Response getStyles(@Context OgcApiApi api, @Context OgcApiRequestContext requestContext) {
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
                            .sorted()
                            .filter(filename -> Files.getFileExtension(filename).equalsIgnoreCase("metadata"))
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
                .orElseThrow(() -> new NotAcceptableException())
                .getStylesResponse(styles, api, requestContext);
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

    private List<OgcApiMediaType> getStylesheetMediaTypes(OgcApiApiDataV2 apiData, File apiDir, String styleId) {
        return extensionRegistry.getExtensionsForType(StyleFormatExtension.class)
                .stream()
                .filter(styleFormatExtension -> styleFormatExtension.isEnabledForApi(apiData))
                .filter(styleFormat -> new File(apiDir + File.separator + styleId + "." + styleFormat.getFileExtension()).exists())
                .map(StyleFormatExtension::getMediaType)
                .collect(Collectors.toList());
    }
}
