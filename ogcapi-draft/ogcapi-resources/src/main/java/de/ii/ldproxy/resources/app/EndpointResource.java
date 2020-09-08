/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources.app;

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
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ldproxy.resources.domain.ResourceFormatExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import static de.ii.ldproxy.ogcapi.domain.FoundationConfiguration.API_RESOURCES_DIR;
import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;

/**
 * fetch list of styles or a style for the service
 */
@Component
@Provides
@Instantiate
public class EndpointResource extends Endpoint {

    @Requires
    I18n i18n;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointResource.class);

    private static final List<String> TAGS = ImmutableList.of("Discover and fetch styles");

    private final java.nio.file.Path resourcesStore;

    public EndpointResource(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext, @Requires ExtensionRegistry extensionRegistry) throws IOException {
        super(extensionRegistry);
        this.resourcesStore = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), API_RESOURCES_DIR)
                                   .resolve("resources");
        Files.createDirectories(resourcesStore);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        Optional<StylesConfiguration> stylesExtension = apiData.getExtension(StylesConfiguration.class);

        if (stylesExtension.isPresent() && stylesExtension.get()
                                                          .getResourcesEnabled()) {
            return true;
        }
        return false;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(ResourceFormatExtension.class);
        return formats;
    }

    @Override
    public ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                    .apiEntrypoint("resources")
                    .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_RESOURCE);
            String path = "/resources/{resourceId}";
            List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
            List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
            if (!pathParameters.stream().filter(param -> param.getName().equals("resourceId")).findAny().isPresent()) {
                LOGGER.error("Path parameter 'resourceId' missing for resource at path '" + path + "'. The GET method will not be available.");
            } else {
                String operationSummary = "fetch the file resource `{resourceId}`";
                Optional<String> operationDescription = Optional.of("Fetches the file resource with identifier `resourceId`. The set of " +
                        "available resources can be retrieved at `/resources`.");
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
     * Fetch a resource by id
     *
     * @param resourceId the local identifier of a specific style
     * @return the style in a json file
     */
    @Path("/{resourceId}")
    @GET
    @Produces(MediaType.WILDCARD)
    public Response getResource(@PathParam("resourceId") String resourceId, @Context OgcApi api,
                                @Context ApiRequestContext requestContext) throws IOException {

        final String datasetId = api.getId();
        java.nio.file.Path apiDir = resourcesStore.resolve(datasetId);
        Files.createDirectories(apiDir);

        java.nio.file.Path resourceFile = apiDir.resolve(resourceId);

        if (Files.notExists(resourceFile)) {
            throw new NotFoundException(MessageFormat.format("The file ''{0}'' does not exist.", resourceId));
        }

        try {
            final byte[] resource = Files.readAllBytes(resourceFile);

            return getFormats().stream()
                    .filter(format -> requestContext.getMediaType().matches(format.getMediaType().type()))
                    .findAny()
                    .map(ResourceFormatExtension.class::cast)
                    .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated.", requestContext.getMediaType().type())))
                    .getResourceResponse(resource, resourceId, api, requestContext);
        } catch (IOException e) {
            throw new ServerErrorException("resource could not be read: "+resourceId, 500);
        }
    }
}
