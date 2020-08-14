/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.google.common.collect.ImmutableList;
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
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * fetch the stylesheet of a style
 */
@Component
@Provides
@Instantiate
public class EndpointStyle extends OgcApiEndpoint {

    @Requires
    I18n i18n;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStyle.class);

    private static final List<String> TAGS = ImmutableList.of("Discover and fetch styles");

    private final File stylesStore;

    public EndpointStyle(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                         @Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
        this.stylesStore = new File(bundleContext.getProperty(DATA_DIR_KEY) + File.separator + "styles");
        if (!stylesStore.exists()) {
            stylesStore.mkdirs();
        }
    }

    private Stream<StyleFormatExtension> getStyleFormatStream(OgcApiApiDataV2 apiData) {
        return extensionRegistry.getExtensionsForType(StyleFormatExtension.class)
                .stream()
                .filter(styleFormatExtension -> styleFormatExtension.isEnabledForApi(apiData));
    }

    private List<OgcApiMediaType> getStylesheetMediaTypes(OgcApiApiDataV2 apiData, File apiDir, String styleId) {
        return getStyleFormatStream(apiData)
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
            formats = extensionRegistry.getExtensionsForType(StyleFormatExtension.class);
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
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_STYLESHEET);
            String path = "/styles/{styleId}";
            List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
            List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
            if (!pathParameters.stream().filter(param -> param.getName().equals("styleId")).findAny().isPresent()) {
                LOGGER.error("Path parameter 'styleId' missing for resource at path '" + path + "'. The GET method will not be available.");
            } else {
                String operationSummary = "fetch a style";
                Optional<String> operationDescription = Optional.of("Fetches the style with identifier `styleId`. " +
                        "The set of available styles can be retrieved at `/styles`. Not all styles are available in " +
                        "all style encodings.");
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
                                                                                  .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", ogcApiRequest.getMediaType())));

        MediaType mediaType = styleFormat.getMediaType().type();
        String key = styleId + "." + styleFormat.getFileExtension();
        String datasetId = dataset.getId();
        File stylesheet = new File( stylesStore + File.separator + datasetId + File.separator + styleId + "." + styleFormat.getFileExtension());
        File metadata = new File( stylesStore + File.separator + datasetId + File.separator + styleId + ".metadata");
        if (!stylesheet.exists()) {
            if (metadata.exists()) {
                throw new NotAcceptableException(MessageFormat.format("The style ''{0}'' is not available in the requested format.", styleId));
            } else {
                throw new NotFoundException(MessageFormat.format("The style ''{0}'' does not exist in this API.", styleId));
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

            List<OgcApiMediaType> alternateMediaTypes = getStylesheetMediaTypes(dataset.getData(), apiDir, Files.getNameWithoutExtension(styleId)).stream()
                    .filter(availableMediaType -> !availableMediaType.matches(styleFormat.getMediaType().type()))
                    .collect(Collectors.toList());
            links = defaultLinkGenerator.generateLinks(ogcApiRequest.getUriCustomizer(), styleFormat.getMediaType(), alternateMediaTypes, i18n, ogcApiRequest.getLanguage());
        }

        try {
            return styleFormat.getStyleResponse(styleId, stylesheet, links, dataset, ogcApiRequest);
        } catch (IOException e) {
            LOGGER.error("Stylesheet in the styles store could not be read: " + styleId);
        }

        throw new ServerErrorException("Error fetching stylesheet: "+styleId, 500);
    }
}
