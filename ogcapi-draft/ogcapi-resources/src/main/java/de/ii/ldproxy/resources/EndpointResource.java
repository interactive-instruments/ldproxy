/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.wfs3.styles.StylesConfiguration;
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
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * fetch list of styles or a style for the service
 */
@Component
@Provides
@Instantiate
public class EndpointResource extends OgcApiEndpoint {

    @Requires
    I18n i18n;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointResource.class);

    private static final List<String> TAGS = ImmutableList.of("Discover and fetch styles");

    private final File resourcesStore; // TODO: change to Store

    public EndpointResource(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext, @Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
        this.resourcesStore = new File(bundleContext.getProperty(DATA_DIR_KEY) + File.separator + "resources");
        if (!resourcesStore.exists()) {
            resourcesStore.mkdirs();
        }
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        Optional<StylesConfiguration> stylesExtension = apiData.getExtension(StylesConfiguration.class);

        if (stylesExtension.isPresent() && stylesExtension.get()
                                                          .getResourcesEnabled()) {
            return true;
        }
        return false;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(ResourceFormatExtension.class);
        return formats;
    }

    @Override
    public OgcApiEndpointDefinition getDefinition(OgcApiApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableOgcApiEndpointDefinition.Builder definitionBuilder = new ImmutableOgcApiEndpointDefinition.Builder()
                    .apiEntrypoint("resources")
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_RESOURCE);
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
     * Fetch a resource by id
     *
     * @param resourceId the local identifier of a specific style
     * @return the style in a json file
     */
    @Path("/{resourceId}")
    @GET
    @Produces(MediaType.WILDCARD)
    public Response getResource(@PathParam("resourceId") String resourceId, @Context OgcApiApi api,
                                @Context OgcApiRequestContext requestContext) {

        final String datasetId = api.getId();
        final File apiDir = new File(resourcesStore + File.separator + datasetId);
        if (!apiDir.exists()) {
            apiDir.mkdirs();
        }

        File resourceFile = new File(apiDir + File.separator + resourceId);

        if (!resourceFile.exists()) {
            throw new NotFoundException();
        }

        try {
            final byte[] resource = Files.readAllBytes(resourceFile.toPath());

            return getFormats().stream()
                    .filter(format -> requestContext.getMediaType().matches(format.getMediaType().type()))
                    .findAny()
                    .map(ResourceFormatExtension.class::cast)
                    .orElseThrow(() -> new NotAcceptableException())
                    .getResourceResponse(resource, resourceId, api, requestContext);
        } catch (IOException e) {
            throw new ServerErrorException("resource could not be read: "+resourceId, 500);
        }
    }
}
