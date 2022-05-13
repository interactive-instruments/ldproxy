/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ogcapi.resources.domain.ImmutableQueryInputResource;
import de.ii.ogcapi.resources.domain.QueriesHandlerResources;
import de.ii.ogcapi.resources.domain.ResourceFormatExtension;
import de.ii.ogcapi.resources.domain.ResourcesConfiguration;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @langEn Fetches the file resource with identifier `resourceId`. The set of
 * available resources can be retrieved at `/resources`.
 * @langDe TODO
 * @name Resource
 * @path /{apiId}/resources/{resourceId}
 * @format {@link de.ii.ogcapi.resources.domain.ResourceFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointResource extends Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointResource.class);

    private static final List<String> TAGS = ImmutableList.of("Discover and fetch other resources");

    private final QueriesHandlerResources queryHandler;

    @Inject
    public EndpointResource(ExtensionRegistry extensionRegistry,
                            QueriesHandlerResources queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return super.isEnabledForApi(apiData) ||
            apiData.getExtension(StylesConfiguration.class).map(StylesConfiguration::isResourcesEnabled).orElse(false);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return ResourcesConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(ResourceFormatExtension.class);
        return formats;
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
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

        return definitionBuilder.build();
    }

    /**
     * Fetch a resource by id
     *
     * @param resourceId the local identifier of a specific resource
     * @return the resources as a file
     */
    @Path("/{resourceId}")
    @GET
    @Produces(MediaType.WILDCARD)
    public Response getResource(@PathParam("resourceId") String resourceId, @Context OgcApi api,
                                @Context ApiRequestContext requestContext) {
        QueriesHandlerResources.QueryInputResource queryInput = ImmutableQueryInputResource.builder()
                                                                                           .from(getGenericQueryInput(api.getData()))
                                                                                           .resourceId(resourceId)
                                                                                           .build();

        return queryHandler.handle(QueriesHandlerResources.Query.RESOURCE, queryInput, requestContext);
    }
}
