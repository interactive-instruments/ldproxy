/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.application;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.api.*;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class OgcApiFeaturesEndpoint extends OgcApiEndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiFeaturesEndpoint.class);
    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("collections")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^/[\\w\\-]+/items(?:/[^/\\s]+)?/?$")
            .build();
    private static final List<String> TAGS = ImmutableList.of("Access data");

    private final OgcApiFeatureCoreProviders providers;
    private final OgcApiFeaturesQuery ogcApiFeaturesQuery;
    private final OgcApiFeaturesCoreQueriesHandler queryHandler;

    public OgcApiFeaturesEndpoint(@Requires OgcApiExtensionRegistry extensionRegistry,
                                  @Requires OgcApiFeatureCoreProviders providers,
                                  @Requires OgcApiFeaturesQuery ogcApiFeaturesQuery,
                                  @Requires OgcApiFeaturesCoreQueriesHandler queryHandler) {
        super(extensionRegistry);
        this.providers = providers;
        this.ogcApiFeaturesQuery = ogcApiFeaturesQuery;
        this.queryHandler = queryHandler;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, OgcApiFeaturesCoreConfiguration.class);
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    /* TODO
    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiApiDataV2 dataset, String subPath) {
        if (subPath.matches("^/[\\w\\-]+/items(?:/[^/\\s]+)?/?$"))
            return extensionRegistry.getExtensionsForType(OgcApiFeatureFormatExtension.class)
                                    .stream()
                                    .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(dataset))
                                    .map(OgcApiFeatureFormatExtension::getMediaType)
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

        if (subPath.matches("^/[\\w\\-]+/items/?$")) {
            // Features
            return new ImmutableSet.Builder<String>()
                    .addAll(OgcApiEndpointExtension.super.getParameters(apiData, subPath))
                    .add("datetime", "bbox", "limit", "offset")
                    .addAll(parametersFromExtensions)
                    .build();
        } else if (subPath.matches("^/[\\w\\-]+/items/[^/\\s]+/?$")) {
            // Feature
            return new ImmutableSet.Builder<String>()
                    .addAll(OgcApiEndpointExtension.super.getParameters(apiData, subPath))
                    .addAll(parametersFromExtensions)
                    .build();
        }

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }
     */

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(OgcApiFeatureFormatExtension.class);
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
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_FEATURES);
            String subSubPath = "/items";
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
                            getQueryParameters(extensionRegistry, apiData, collectionId, path) :
                            getQueryParameters(extensionRegistry, apiData, path);
                    final String operationSummary = "retrieve features in the feature collection '" + collectionId + "'";
                    Optional<String> operationDescription = Optional.of("The response is a document consisting of features in the collection. " +
                            "The features included in the response are determined by the server based on the query parameters of the request. " +
                            "To support access to larger collections without overloading the client, the API supports paged access with links " +
                            "to the next page, if more features are selected that the page size. The `bbox` and `datetime` parameter can be " +
                            "used to select only a subset of the features in the collection (the features that are in the bounding box or time interval). " +
                            "The `bbox` parameter matches all features in the collection that are not associated with a location, too. " +
                            "The `datetime` parameter matches all features in the collection that are not associated with a time stamp or interval, too. " +
                            "The `limit` parameter may be used to control the subset of the selected features that should be returned in the response, " +
                            "the page size. Each page may include information about the number of selected and returned features (`numberMatched` " +
                            "and `numberReturned`) as well as links to support paging (link relation `next`).");
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
            subSubPath = "/items/{featureId}";
            path = "/collections/{collectionId}" + subSubPath;
            pathParameters = getPathParameters(extensionRegistry, apiData, path);
            optCollectionIdParam = pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
            if (!optCollectionIdParam.isPresent()) {
                LOGGER.error("Path parameter 'collectionId' missing for resource at path '" + path + "'. The resource will not be available.");
            } else {
                final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
                final boolean explode = collectionIdParam.getExplodeInOpenApi();
                final Set<String> collectionIds = explode ?
                        collectionIdParam.getValues(apiData) :
                        ImmutableSet.of("{collectionId}");
                for (String collectionId : collectionIds) {
                    final Set<OgcApiQueryParameter> queryParameters = explode ?
                            getQueryParameters(extensionRegistry, apiData, collectionId, path) :
                            getQueryParameters(extensionRegistry, apiData, path);
                    final String operationSummary = "retrieve a feature in the feature collection '" + collectionId + "'";
                    final Optional<String> operationDescription = Optional.of("Fetch the feature with id `{featureId}`.");
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

    @Override
    public ImmutableSet<OgcApiQueryParameter> getQueryParameters(OgcApiExtensionRegistry extensionRegistry, OgcApiApiDataV2 apiData, String collectionId, String definitionPath) {
        ImmutableSet<OgcApiQueryParameter> generalList = super.getQueryParameters(extensionRegistry, apiData, collectionId, definitionPath);

        FeatureTypeConfigurationOgcApi featureType = apiData.getCollections().get(collectionId);
        Optional<OgcApiFeaturesCoreConfiguration> coreConfiguration = featureType.getExtension(OgcApiFeaturesCoreConfiguration.class);

        final Map<String, String> filterableFields = coreConfiguration.map(OgcApiFeaturesCoreConfiguration::getOtherFilterParameters)
                .orElse(ImmutableMap.of());

        Map<String, FeatureTypeMapping2> transformations;
        if (coreConfiguration.isPresent()) {
            transformations = coreConfiguration.get().getTransformations();
            // TODO
        }

        if (definitionPath.equals("/collections/{collectionId}/items"))
            return ImmutableSet.<OgcApiQueryParameter>builder()
                    .addAll(generalList)
                    .addAll(filterableFields.keySet().stream()
                        .map(field -> new ImmutableOgcApiQueryParameterTemplateQueryable.Builder()
                                    .apiId(apiData.getId())
                                    .collectionId(collectionId)
                                    .name(field)
                                    .description("Filter the collection by property '" + field + "'")
                                    .schema(new StringSchema())
                                    .build())
                        .collect(Collectors.toSet()))
                    .build();

        return generalList;
    }

    @GET
    @Path("/{collectionId}/items")
    public Response getItems(@Auth Optional<User> optionalUser,
                             @Context OgcApiApi api,
                             @Context OgcApiRequestContext requestContext,
                             @Context UriInfo uriInfo,
                             @PathParam("collectionId") String collectionId) {
        checkAuthorization(api.getData(), optionalUser);
        checkCollectionExists(api.getData(), collectionId);

        FeatureTypeConfigurationOgcApi collectionData = api.getData()
                                                       .getCollections()
                                                       .get(collectionId);

        OgcApiFeaturesCoreConfiguration coreConfiguration = getExtensionConfiguration(api.getData(), collectionData, OgcApiFeaturesCoreConfiguration.class).orElseThrow(NotFoundException::new);

        int minimumPageSize = coreConfiguration.getMinimumPageSize();
        int defaultPageSize = coreConfiguration.getDefaultPageSize();
        int maxPageSize = coreConfiguration.getMaxPageSize();
        boolean showsFeatureSelfLink = coreConfiguration.getShowsFeatureSelfLink();
        boolean includeHomeLink = getExtensionConfiguration(api.getData(), OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeHomeLink)
                .orElse(false);
        boolean includeLinkHeader = getExtensionConfiguration(api.getData(), OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeLinkHeader)
                .orElse(false);

        Set<OgcApiQueryParameter> allowedParameters = getQueryParameters(extensionRegistry, api.getData(), "/collections/{collectionId}/items");
        FeatureQuery query = ogcApiFeaturesQuery.requestToFeatureQuery(api.getData(), collectionData, coreConfiguration, minimumPageSize, defaultPageSize, maxPageSize, toFlatMap(uriInfo.getQueryParameters()), allowedParameters);

        OgcApiFeaturesCoreQueriesHandlerImpl.OgcApiQueryInputFeatures queryInput = new ImmutableOgcApiQueryInputFeatures.Builder()
                .collectionId(collectionId)
                .query(query)
                .featureProvider(providers.getFeatureProvider(api.getData(), collectionData))
                .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
                .defaultPageSize(Optional.of(defaultPageSize))
                .showsFeatureSelfLink(showsFeatureSelfLink)
                .includeHomeLink(includeHomeLink)
                .includeLinkHeader(includeLinkHeader)
                .build();

        return queryHandler.handle(OgcApiFeaturesCoreQueriesHandlerImpl.Query.FEATURES, queryInput, requestContext);
    }

    @GET
    @Path("/{collectionId}/items/{featureId}")
    public Response getItem(@Auth Optional<User> optionalUser,
                            @Context OgcApiApi api,
                            @Context OgcApiRequestContext requestContext,
                            @Context UriInfo uriInfo,
                            @PathParam("collectionId") String collectionId,
                            @PathParam("featureId") String featureId) {
        checkAuthorization(api.getData(), optionalUser);
        checkCollectionExists(api.getData(), collectionId);

        FeatureTypeConfigurationOgcApi collectionData = api.getData()
                                                           .getCollections()
                                                           .get(collectionId);

        OgcApiFeaturesCoreConfiguration coreConfiguration = getExtensionConfiguration(api.getData(), collectionData, OgcApiFeaturesCoreConfiguration.class).orElseThrow(NotFoundException::new);

        boolean includeHomeLink = getExtensionConfiguration(api.getData(), OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeHomeLink)
                .orElse(false);
        boolean includeLinkHeader = getExtensionConfiguration(api.getData(), OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeLinkHeader)
                .orElse(false);

        Set<OgcApiQueryParameter> allowedParameters = getQueryParameters(extensionRegistry, api.getData(), "/collections/{collectionId}/items/{featureId}");
        FeatureQuery query = ogcApiFeaturesQuery.requestToFeatureQuery(api.getData(), collectionData, coreConfiguration, toFlatMap(uriInfo.getQueryParameters()), allowedParameters, featureId);

        OgcApiFeaturesCoreQueriesHandlerImpl.OgcApiQueryInputFeature queryInput = new ImmutableOgcApiQueryInputFeature.Builder()
                .collectionId(collectionId)
                .featureId(featureId)
                .query(query)
                .featureProvider(providers.getFeatureProvider(api.getData(), collectionData))
                .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
                .includeHomeLink(includeHomeLink)
                .includeLinkHeader(includeLinkHeader)
                .build();

        return queryHandler.handle(OgcApiFeaturesCoreQueriesHandlerImpl.Query.FEATURE, queryInput, requestContext);
    }
}
