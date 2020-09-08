/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package ii.de.ldproxy.resources.manager;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.Endpoint;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ldproxy.resources.domain.ResourceFormatExtension;
import de.ii.xtraplatform.auth.domain.User;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.ii.ldproxy.ogcapi.domain.FoundationConfiguration.API_RESOURCES_DIR;
import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;

/**
 * creates, updates and deletes a resource from the service
 */
@Component
@Provides
@Instantiate
public class EndpointResourcesManager extends Endpoint implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointResourcesManager.class);
    private static final List<String> TAGS = ImmutableList.of("Create, update and delete styles");

    private final java.nio.file.Path resourcesStore;

    public EndpointResourcesManager(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                                 @Requires ExtensionRegistry extensionRegistry) throws IOException {
        super(extensionRegistry);

        this.resourcesStore = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), API_RESOURCES_DIR)
                                   .resolve("resources");
        Files.createDirectories(resourcesStore);
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-styles-1/0.0/conf/manage-resources");
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        Optional<StylesConfiguration> stylesExtension = apiData.getExtension(StylesConfiguration.class);

        if (stylesExtension.isPresent() && stylesExtension.get()
                                                          .getResourceManagerEnabled()) {
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
            formats = extensionRegistry.getExtensionsForType(ResourceFormatExtension.class)
                    .stream()
                    .filter(ResourceFormatExtension::canSupportTransactions)
                    .collect(Collectors.toList());
        return formats;
    }

    private Map<MediaType, ApiMediaTypeContent> getRequestContent(OgcApiDataV2 apiData, String path, HttpMethods method) {
        return getFormats().stream()
                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
                .map(f -> f.getRequestContent(apiData, path, method))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(c -> c.getOgcApiMediaType().type(),c -> c));
    }

    @Override
    public ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            Optional<StylesConfiguration> stylesExtension = apiData.getExtension(StylesConfiguration.class);
            ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                    .apiEntrypoint("resources")
                    .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_RESOURCES_MANAGER);
            String path = "/resources/{resourceId}";
            HttpMethods method = HttpMethods.PUT;
            List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
            List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path, method);
            String operationSummary = "replace a file resource or add a new one";
            Optional<String> operationDescription = Optional.of("Replace an existing resource with the id `resourceId`. If no " +
                    "such resource exists, a new resource with that id is added. " +
                    "A sprite used in a Mapbox Style stylesheet consists of " +
                    "three resources. Each of the resources needs to be created " +
                    "(and eventually deleted) separately.\n" +
                    "The PNG bitmap image (resourceId ends in '.png'), the JSON " +
                    "index file (resourceId of the same name, but ends in '.json' " +
                    "instead of '.png') and the PNG  bitmap image for " +
                    "high-resolution displays (the file ends in '.@2x.png').\n" +
                    "The resource will only by available in the native format in " +
                    "which the resource is posted. There is no support for " +
                    "automated conversions to other representations.");
            ImmutableOgcApiResourceData.Builder resourceBuilder = new ImmutableOgcApiResourceData.Builder()
                    .path(path)
                    .pathParameters(pathParameters);
            Map<MediaType, ApiMediaTypeContent> requestContent = getRequestContent(apiData, path, method);
            ApiOperation operation = addOperation(apiData, method, requestContent, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilder.putOperations(method.name(), operation);
            method = HttpMethods.DELETE;
            queryParameters = getQueryParameters(extensionRegistry, apiData, path, method);
            operationSummary = "delete a file resource";
            operationDescription = Optional.of("Delete an existing resource with the id `resourceId`. If no " +
                    "such resource exists, an error is returned.");
            requestContent = getRequestContent(apiData, path, method);
            operation = addOperation(apiData, method, requestContent, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilder.putOperations(method.name(), operation);
            definitionBuilder.putResources(path, resourceBuilder.build());

            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
    }

    /**
     * create or update a resource
     *
     * @param resourceId the local identifier of a specific style
     * @return empty response (204)
     */
    @Path("/{resourceId}")
    @PUT
    @Consumes(MediaType.WILDCARD)
    public Response putResource(@Auth Optional<User> optionalUser, @PathParam("resourceId") String resourceId,
                                @Context OgcApi api, @Context ApiRequestContext requestContext,
                                @Context HttpServletRequest request, byte[] requestBody) throws IOException {

        checkAuthorization(api.getData(), optionalUser);

        return getFormats().stream()
                .filter(format -> requestContext.getMediaType().matches(format.getMediaType().type()))
                .findAny()
                .map(ResourceFormatExtension.class::cast)
                .orElseThrow(() -> new NotSupportedException(MessageFormat.format("The provided media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())))
                .putResource(resourcesStore, requestBody, resourceId, api, requestContext);
    }

    /**
     * deletes a resource
     *
     * @param resourceId the local identifier of a specific style
     * @return empty response (204)
     */
    @Path("/{resourceId}")
    @DELETE
    public Response deleteResource(@Auth Optional<User> optionalUser, @PathParam("resourceId") String resourceId,
                                @Context OgcApi dataset) {

        checkAuthorization(dataset.getData(), optionalUser);

        final String datasetId = dataset.getId();
        File apiDir = new File(resourcesStore + File.separator + datasetId);
        if (!apiDir.exists()) {
            apiDir.mkdirs();
        }

        File resourceFile = new File(apiDir + File.separator + resourceId);
        if (resourceFile.exists())
            resourceFile.delete();

        return Response.noContent()
                       .build();
    }

}
