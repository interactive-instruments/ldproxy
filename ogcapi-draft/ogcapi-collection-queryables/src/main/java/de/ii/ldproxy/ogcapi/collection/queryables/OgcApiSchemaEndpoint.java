/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collection.queryables;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.auth.api.User;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Provides
@Instantiate
public class OgcApiSchemaEndpoint extends OgcApiEndpointSubCollection implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiSchemaEndpoint.class);
    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("collections")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^/[\\w\\-]+/schema/?$")
            .build();

    private static final List<String> TAGS = ImmutableList.of("Discover data collections");

    @Requires
    private OgcApiQueryablesQueriesHandler queryHandler;

    public OgcApiSchemaEndpoint(@Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://ldproxy.net/tbd/1.0/conf/schema");
    }

    /*
    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiApiDataV2 dataset, String subPath) {
        if (subPath.matches("^/[\\w\\-]+/schema/?$"))
            return extensionRegistry.getExtensionsForType(OgcApiSchemaFormatExtension.class)
                                    .stream()
                                    .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(dataset))
                                    .map(OgcApiSchemaFormatExtension::getMediaType)
                                    .collect(ImmutableSet.toImmutableSet());

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }

    @Override
    public ImmutableSet<String> getParameters(OgcApiApiDataV2 apiData, String subPath) {
        if (!isEnabledForApi(apiData))
            return ImmutableSet.of();

        ImmutableSet<String> parametersFromExtensions = new ImmutableSet.Builder<String>()
            .addAll(extensionRegistry.getExtensionsForType(OgcApiParameterExtension.class)
                .stream()
                .map(ext -> ext.getParameters(apiData, subPath))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet()))
            .build();

        if (subPath.matches("^/[\\w\\-]+/schema/?$")) {
            // Schema
            return new ImmutableSet.Builder<String>()
                    .addAll(OgcApiEndpointExtension.super.getParameters(apiData, subPath))
                    .addAll(parametersFromExtensions)
                    .build();
        }

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }
     */

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, QueryablesConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData, apiData.getCollections().get(collectionId), QueryablesConfiguration.class);
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(OgcApiSchemaFormatExtension.class);
        return formats;
    }

    @Override
    public OgcApiEndpointDefinition getDefinition(OgcApiApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableOgcApiEndpointDefinition.Builder definitionBuilder = new ImmutableOgcApiEndpointDefinition.Builder()
                    .apiEntrypoint("collections")
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_SCHEMA);
            String subSubPath = "/schema";
            String path = "/collections/{collectionId}" + subSubPath;
            Set<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
            Optional<OgcApiPathParameter> optCollectionIdParam = pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
            if (!optCollectionIdParam.isPresent()) {
                LOGGER.error("Path parameter 'collectionId' missing for resource at path '" + path + "'. The resource will not be available.");
            } else {
                final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
                final boolean explode = collectionIdParam.getExplodeInOpenApi();
                final Set<String> collectionIds = (explode) ?
                        collectionIdParam.getValues(apiData) :
                        ImmutableSet.of("{collectionId}");
                for (String collectionId : collectionIds) {
                    final Set<OgcApiQueryParameter> queryParameters = explode ?
                            getQueryParameters(extensionRegistry, apiData, path, collectionId) :
                            getQueryParameters(extensionRegistry, apiData, path);
                    final String operationSummary = "retrieve the schema of features in the feature collection '" + collectionId + "'";
                    Optional<String> operationDescription = Optional.empty(); // TODO
                    String resourcePath = "/collections/" + collectionId + subSubPath;
                    ImmutableOgcApiResourceData.Builder resourceBuilder = new ImmutableOgcApiResourceData.Builder()
                            .path(resourcePath)
                            .pathParameters(pathParameters);
                    OgcApiOperation operation = addOperation(apiData, OgcApiContext.HttpMethods.GET, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, TAGS);
                    if (operation!=null)
                        resourceBuilder.putOperations("GET", operation);
                    definitionBuilder.putResources(resourcePath, resourceBuilder.build());
                }
            }
            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
    }

    @GET
    @Path("/{collectionId}/schema")
    @Produces("application/schema+json")
    public Response getSchema(@Auth Optional<User> optionalUser,
                             @Context OgcApiApi api,
                             @Context OgcApiRequestContext requestContext,
                             @Context UriInfo uriInfo,
                             @PathParam("collectionId") String collectionId) {
        checkAuthorization(api.getData(), optionalUser);

        boolean includeHomeLink = getExtensionConfiguration(api.getData(), OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeHomeLink)
                .orElse(false);
        boolean includeLinkHeader = getExtensionConfiguration(api.getData(), OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeLinkHeader)
                .orElse(false);

        OgcApiQueryablesQueriesHandler.OgcApiQueryInputQueryables queryInput = new ImmutableOgcApiQueryInputQueryables.Builder()
                .collectionId(collectionId)
                .includeHomeLink(includeHomeLink)
                .includeLinkHeader(includeLinkHeader)
                .build();

        return queryHandler.handle(OgcApiQueryablesQueriesHandler.Query.SCHEMA, queryInput, requestContext);
    }
}
