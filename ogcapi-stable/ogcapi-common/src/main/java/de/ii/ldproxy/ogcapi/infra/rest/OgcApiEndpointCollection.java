/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.application.ImmutableOgcApiQueryInputFeatureCollection;
import de.ii.ldproxy.ogcapi.application.OgcApiQueriesHandlerCollections;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Provides
@Instantiate
public class OgcApiEndpointCollection extends OgcApiEndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiEndpointCollection.class);
    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("collections")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^/[\\w\\-]+/?$")
            .build();
    private static final List<String> TAGS = ImmutableList.of("Discover data collections");

    @Requires
    private OgcApiQueriesHandlerCollections queryHandler;

    public OgcApiEndpointCollection(@Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, OgcApiCommonConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData, apiData.getCollections().get(collectionId), OgcApiCommonConfiguration.class);
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(CollectionsFormatExtension.class);
        return formats;
    }

    /*
    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiApiDataV2 dataset, String subPath) {
        if (subPath.matches("^/[\\w\\-]+/?$"))
            return extensionRegistry.getExtensionsForType(CollectionsFormatExtension.class)
                                    .stream()
                                    .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(dataset))
                                    .map(CollectionsFormatExtension::getMediaType)
                                    .collect(ImmutableSet.toImmutableSet());

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }
     */

    @Override
    public OgcApiEndpointDefinition getDefinition(OgcApiApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableOgcApiEndpointDefinition.Builder definitionBuilder = new ImmutableOgcApiEndpointDefinition.Builder()
                    .apiEntrypoint("collections")
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_COLLECTION);
            String path = "/collections/{collectionId}";
            Set<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
            Set<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
            Optional<OgcApiPathParameter> optCollectionIdParam = pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
            if (!optCollectionIdParam.isPresent()) {
                LOGGER.error("Path parameter 'collectionId' missing for resource at path '" + path + "'. The GET method will not be available.");
            } else {
                final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
                final boolean explode = collectionIdParam.getExplodeInOpenApi();
                final Set<String> collectionIds = (explode) ?
                        collectionIdParam.getValues(apiData) :
                        ImmutableSet.of("{collectionId}");
                for (String collectionId : collectionIds) {
                    FeatureTypeConfigurationOgcApi featureType = apiData.getCollections()
                            .get(collectionId);
                    String operationSummary = "feature collection '" + featureType.getLabel() + "'";
                    Optional<String> operationDescription = Optional.of("Information about the feature collection with " +
                            "id '"+collectionId+"'. The response contains a link to the items in the collection " +
                            "(path `/collections/{collectionId}/items`,link relation `items`) as well as key " +
                            "information about the collection. This information includes:\n\n" +
                            "* A local identifier for the collection that is unique for the dataset;\n" +
                            "* A list of coordinate reference systems (CRS) in which geometries may be returned by the server. " +
                            "The first CRS is the default coordinate reference system (the default is always WGS 84 with " +
                            "axis order longitude/latitude);\n" +
                            "* An optional title and description for the collection;\n" +
                            "* An optional extent that can be used to provide an indication of the spatial and temporal extent " +
                            "of the collection - typically derived from the data;\n" +
                            "* An optional indicator about the type of the items in the collection (the default value, " +
                            "if the indicator is not provided, is 'feature').");
                    String resourcePath = "/collections/" + collectionId;
                    ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                            .path(resourcePath)
                            .pathParameters(pathParameters);
                    OgcApiOperation operation = addOperation(apiData, OgcApiContext.HttpMethods.GET, queryParameters, collectionId, "", operationSummary, operationDescription, TAGS);
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
    @Path("/{collectionId}")
    public Response getCollection(@Auth Optional<User> optionalUser, @Context OgcApiApi api,
                                   @Context OgcApiRequestContext requestContext, @PathParam("collectionId") String collectionId) {
        checkAuthorization(api.getData(), optionalUser);

        boolean includeHomeLink = getExtensionConfiguration(api.getData(), OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeHomeLink)
                .orElse(false);
        boolean includeLinkHeader = getExtensionConfiguration(api.getData(), OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeLinkHeader)
                .orElse(false);

        OgcApiQueriesHandlerCollections.OgcApiQueryInputFeatureCollection queryInput = new ImmutableOgcApiQueryInputFeatureCollection.Builder()
                .collectionId(collectionId)
                .includeHomeLink(includeHomeLink)
                .includeLinkHeader(includeLinkHeader)
                .build();

        return queryHandler.handle(OgcApiQueriesHandlerCollections.Query.FEATURE_COLLECTION, queryInput, requestContext);
    }
}
