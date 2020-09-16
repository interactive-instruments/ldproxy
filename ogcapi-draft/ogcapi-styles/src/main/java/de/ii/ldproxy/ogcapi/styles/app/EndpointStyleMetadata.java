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
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.Endpoint;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ImmutableLink;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableStyleLayer;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableStyleMetadata;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableStyleSheet;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadata;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadataFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleSheet;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.StylesLinkGenerator;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.ii.ldproxy.ogcapi.domain.FoundationConfiguration.API_RESOURCES_DIR;
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

    private final java.nio.file.Path stylesStore;

    public EndpointStyleMetadata(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                                 @Requires ExtensionRegistry extensionRegistry) throws IOException {
        super(extensionRegistry);
        this.stylesStore = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), API_RESOURCES_DIR)
                                .resolve("styles");
        Files.createDirectories(stylesStore);
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
            final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();
            List<StyleSheet> stylesheets = new ArrayList<>();
            stylesheets.addAll(extensionRegistry.getExtensionsForType(StyleFormatExtension.class)
                                                .stream()
                                                .filter(format -> !format.getDerived())
                                                .filter(format -> Paths.get(stylesStore.toString(),apiId)
                                                                       .resolve(styleId+"."+format.getFileExtension())
                                                                       .toFile()
                                                                       .exists())
                                                .map(format -> ImmutableStyleSheet.builder()
                                                                                  .native_(true)
                                                                                  .title(styleId)
                                                                                  .link(stylesLinkGenerator.generateStylesheetLink(requestContext.getUriCustomizer(),
                                                                                                                                   styleId, format.getMediaType(),
                                                                                                                                   i18n, requestContext.getLanguage()))
                                                                                  .specification(format.getSpecification())
                                                                                  .version(format.getVersion())
                                                                                  .build())
                                                .collect(Collectors.toList()));
            ImmutableStyleMetadata.Builder metadata = ImmutableStyleMetadata.builder()
                                                                            .id(styleId)
                                                                            .title(styleId)
                                                                            .stylesheets(stylesheets);

            return metadata.build();
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

                return replaceParameters(metadata, apiId, api.getData().getApiVersion(), requestContext.getUriCustomizer().copy());
            } catch (IOException e) {
                LOGGER.error("Style metadata file in styles store is invalid: "+metadataFile.getAbsolutePath());
                throw new InternalServerErrorException(MessageFormat.format("Style metadata file in styles store is invalid for style ''{0}'' in API ''{1}''.", styleId, api.getId()), e);
            }
        } catch (IOException e) {
            LOGGER.error("Style metadata could not be read: "+styleId);
            throw new InternalServerErrorException(MessageFormat.format("Style metadata could not be read for style ''{0}'' in API ''{1}''.", styleId, api.getId()), e);
        }
    }

    private StyleMetadata replaceParameters(StyleMetadata metadata, String apiId, Optional<Integer> apiVersion, URICustomizer uriCustomizer) {

        // any template parameters in links?
        boolean templated = metadata.getStylesheets()
                                    .orElse(ImmutableList.of())
                                    .stream()
                                    .map(styleSheet -> styleSheet.getLink().orElse(null))
                                    .filter(Objects::nonNull)
                                    .anyMatch(link -> Objects.requireNonNullElse(link.getTemplated(),false) && link.getHref().matches("^.*\\{serviceUrl\\}.*$")) ||
                            metadata.getLayers()
                                    .orElse(ImmutableList.of())
                                    .stream()
                                    .map(layer -> layer.getSampleData().orElse(null))
                                    .filter(Objects::nonNull)
                                    .anyMatch(link -> Objects.requireNonNullElse(link.getTemplated(),false) && link.getHref().matches("^.*\\{serviceUrl\\}.*$")) ||
                            metadata.getLinks()
                                    .stream()
                                    .anyMatch(link -> Objects.requireNonNullElse(link.getTemplated(),false) && link.getHref().matches("^.*\\{serviceUrl\\}.*$"));

        if (!templated)
            return metadata;

        String serviceUrl = uriCustomizer.removeLastPathSegments(3)
                                         .clearParameters()
                                         .ensureNoTrailingSlash()
                                         .toString();

        return ImmutableStyleMetadata.builder()
                                     .from(metadata)
                                     .stylesheets(metadata.getStylesheets()
                                                          .orElse(ImmutableList.of())
                                                          .stream()
                                                          .map(styleSheet -> ImmutableStyleSheet.builder()
                                                                                                .from(styleSheet)
                                                                                                .link(!(styleSheet.getLink().isPresent()) ?
                                                                                                              Optional.empty() :
                                                                                                              Objects.requireNonNullElse(styleSheet.getLink().get().getTemplated(), false) ?
                                                                                                                      Optional.of(new ImmutableLink.Builder()
                                                                                                                                       .from(styleSheet.getLink().get())
                                                                                                                                       .href(styleSheet.getLink().get()
                                                                                                                                                       .getHref()
                                                                                                                                                       .replace("{serviceUrl}", serviceUrl))
                                                                                                                                       .templated(null)
                                                                                                                                       .build()) :
                                                                                                                      styleSheet.getLink())
                                                                                                .build())
                                                          .collect(ImmutableList.toImmutableList()))
                                     .layers(metadata.getLayers()
                                                     .orElse(ImmutableList.of())
                                                     .stream()
                                                     .map(layer -> ImmutableStyleLayer.builder()
                                                                                      .from(layer)
                                                                                      .sampleData(!(layer.getSampleData().isPresent()) ?
                                                                                                          Optional.empty() :
                                                                                                          Objects.requireNonNullElse(layer.getSampleData().get().getTemplated(), false) ?
                                                                                                                  Optional.of(new ImmutableLink.Builder()
                                                                                                                                      .from(layer.getSampleData()
                                                                                                                                                 .get())
                                                                                                                                      .href(layer.getSampleData()
                                                                                                                                                 .get()
                                                                                                                                                 .getHref()
                                                                                                                                                 .replace("{serviceUrl}", serviceUrl))
                                                                                                                                      .templated(null)
                                                                                                                                      .build()) :
                                                                                                                  layer.getSampleData())
                                                                                      .build())
                                                     .collect(ImmutableList.toImmutableList()))
                                     .links(metadata.getLinks()
                                                    .stream()
                                                    .map(link -> new ImmutableLink.Builder()
                                                                                  .from(link)
                                                                                  .href(link.getHref()
                                                                                            .replace("{serviceUrl}", serviceUrl))
                                                                                  .templated(null)
                                                                                  .build())
                                                    .collect(ImmutableList.toImmutableList()))
                                     .build();
    }
}
