/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package ii.de.ldproxy.resources.manager;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext.HttpMethods;
import de.ii.ldproxy.resources.ResourceFormatExtension;
import de.ii.ldproxy.wfs3.styles.StylesConfiguration;
import de.ii.xtraplatform.auth.api.User;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * creates, updates and deletes a resource from the service
 */
@Component
@Provides
@Instantiate
public class EndpointResourcesManager extends OgcApiEndpoint implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointResourcesManager.class);
    private static final List<String> TAGS = ImmutableList.of("Create, update and delete styles");

    private final File resourcesStore;

    public EndpointResourcesManager(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                                 @Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
        this.resourcesStore = new File(bundleContext.getProperty(DATA_DIR_KEY) + File.separator + "resources");
        if (!resourcesStore.exists()) {
            resourcesStore.mkdirs();
        }
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/t15/opf-styles-1/1.0/conf/manage-resources");
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        Optional<StylesConfiguration> stylesExtension = getExtensionConfiguration(apiData, StylesConfiguration.class);

        if (stylesExtension.isPresent() && stylesExtension.get()
                                                          .getResourceManagerEnabled()) {
            return true;
        }
        return false;
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

    private Map<MediaType, OgcApiMediaTypeContent> getRequestContent(OgcApiApiDataV2 apiData, String path, OgcApiContext.HttpMethods method) {
        return getFormats().stream()
                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
                .map(f -> f.getRequestContent(apiData, path, method))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(c -> c.getOgcApiMediaType().type(),c -> c));
    }

    @Override
    public OgcApiEndpointDefinition getDefinition(OgcApiApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            Optional<StylesConfiguration> stylesExtension = getExtensionConfiguration(apiData, StylesConfiguration.class);
            ImmutableOgcApiEndpointDefinition.Builder definitionBuilder = new ImmutableOgcApiEndpointDefinition.Builder()
                    .apiEntrypoint("resources")
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_RESOURCES_MANAGER);
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
            Map<MediaType, OgcApiMediaTypeContent> requestContent = getRequestContent(apiData, path, method);
            OgcApiOperation operation = addOperation(apiData, method, requestContent, queryParameters, path, operationSummary, operationDescription, TAGS);
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
                                @Context OgcApiApi api, @Context OgcApiRequestContext requestContext,
                                @Context HttpServletRequest request, byte[] requestBody) {

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
                                @Context OgcApiApi dataset) {

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
