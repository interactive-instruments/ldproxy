/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.transactional;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiHeader;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureTransactions;
import io.dropwizard.auth.Auth;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class EndpointTransactional extends EndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointTransactional.class);
    private static final List<String> TAGS = ImmutableList.of("Mutate data");

    private final FeaturesCoreProviders providers;
    private final CommandHandlerTransactional commandHandler;

    @Inject
    public EndpointTransactional(ExtensionRegistry extensionRegistry,
                                 FeaturesCoreProviders providers) {
        super(extensionRegistry);
        this.providers = providers;
        this.commandHandler = new CommandHandlerTransactional();
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TransactionalConfiguration.class;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return super.isEnabledForApi(apiData) && providers.getFeatureProvider(apiData)
                                                          .map(FeatureProvider2::supportsTransactions)
                                                          .orElse(false);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return super.isEnabledForApi(apiData, collectionId) && providers.getFeatureProvider(apiData)
                                                                        .map(FeatureProvider2::supportsTransactions)
                                                                        .orElse(false);
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(FeatureFormatExtension.class)
                    .stream()
                    .filter(FeatureFormatExtension::canSupportTransactions)
                    .collect(Collectors.toList());
        return formats;
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("collections")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_FEATURES_TRANSACTION);
        String subSubPath = "/items";
        String path = "/collections/{collectionId}" + subSubPath;
        List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
        Optional<OgcApiPathParameter> optCollectionIdParam = pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
        if (!optCollectionIdParam.isPresent()) {
            LOGGER.error("Path parameter 'collectionId' missing for resource at path '" + path + "'. The resource will not be available.");
        } else {
            final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
            final boolean explode = collectionIdParam.getExplodeInOpenApi(apiData);
            final List<String> collectionIds = (explode) ?
                    collectionIdParam.getValues(apiData) :
                    ImmutableList.of("{collectionId}");
            for (String collectionId : collectionIds) {
                final List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path, collectionId, HttpMethods.POST);
                final List<ApiHeader> headers = getHeaders(extensionRegistry, apiData, path, collectionId, HttpMethods.POST);
                final String operationSummary = "add a feature in the feature collection '" + collectionId + "'";
                Optional<String> operationDescription = Optional.of("The content of the request is a new feature in one of the supported encodings. The URI of the new feature is returned in the header `Location`.");
                String resourcePath = "/collections/" + collectionId + subSubPath;
                ImmutableOgcApiResourceData.Builder resourceBuilder = new ImmutableOgcApiResourceData.Builder()
                        .path(resourcePath)
                        .pathParameters(pathParameters);
                ApiOperation operation = addOperation(apiData, HttpMethods.POST, queryParameters, headers, collectionId, subSubPath, operationSummary, operationDescription, TAGS);
                if (operation!=null)
                    resourceBuilder.putOperations("POST", operation);
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
            final boolean explode = collectionIdParam.getExplodeInOpenApi(apiData);
            final List<String> collectionIds = explode ?
                    collectionIdParam.getValues(apiData) :
                    ImmutableList.of("{collectionId}");
            for (String collectionId : collectionIds) {
                List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path, collectionId, HttpMethods.PUT);
                List<ApiHeader> headers = getHeaders(extensionRegistry, apiData, path, collectionId, HttpMethods.PUT);
                String operationSummary = "add or update a feature in the feature collection '" + collectionId + "'";
                Optional<String> operationDescription = Optional.of("The content of the request is a new feature in one of the supported encodings. The id of the new or updated feature is `{featureId}`.");
                String resourcePath = "/collections/" + collectionId + subSubPath;
                ImmutableOgcApiResourceData.Builder resourceBuilder = new ImmutableOgcApiResourceData.Builder()
                        .path(resourcePath)
                        .pathParameters(pathParameters);
                ApiOperation operation = addOperation(apiData, HttpMethods.PUT, queryParameters, headers, collectionId, subSubPath, operationSummary, operationDescription, TAGS);
                if (operation!=null)
                    resourceBuilder.putOperations("PUT", operation);
                queryParameters = getQueryParameters(extensionRegistry, apiData, path, collectionId, HttpMethods.DELETE);
                headers = getHeaders(extensionRegistry, apiData, path, collectionId, HttpMethods.DELETE);
                operationSummary = "delete a feature in the feature collection '" + collectionId + "'";
                operationDescription = Optional.of("The feature with id `{featureId}` will be deleted.");
                operation = addOperation(apiData, HttpMethods.DELETE, queryParameters, headers, collectionId, subSubPath, operationSummary, operationDescription, TAGS);
                if (operation!=null)
                    resourceBuilder.putOperations("DELETE", operation);
                definitionBuilder.putResources(resourcePath, resourceBuilder.build());
            }

        }
        return definitionBuilder.build();
    }

    @Path("/{id}/items")
    @POST
    @Consumes("application/geo+json")
    public Response postItems(@Auth Optional<User> optionalUser, @PathParam("id") String id,
                              @Context OgcApi service, @Context ApiRequestContext apiRequestContext,
                              @Context HttpServletRequest request, InputStream requestBody) {
        FeatureProvider2 featureProvider = providers.getFeatureProviderOrThrow(service.getData(), service.getData().getCollections().get(id));

        checkTransactional(featureProvider);

        checkAuthorization(service.getData(), optionalUser);


        return commandHandler.postItemsResponse((FeatureTransactions) featureProvider, apiRequestContext.getMediaType(), apiRequestContext.getUriCustomizer()
                                                                                                                                          .copy(), id, requestBody);
    }

    @Path("/{id}/items/{featureid}")
    @PUT
    @Consumes("application/geo+json")
    public Response putItem(@Auth Optional<User> optionalUser, @PathParam("id") String id,
                            @PathParam("featureid") final String featureId, @Context OgcApi service,
                            @Context ApiRequestContext apiRequestContext, @Context HttpServletRequest request,
                            InputStream requestBody) {

        FeatureProvider2 featureProvider = providers.getFeatureProviderOrThrow(service.getData(), service.getData().getCollections().get(id));

        checkTransactional(featureProvider);

        checkAuthorization(service.getData(), optionalUser);

        return commandHandler.putItemResponse((FeatureTransactions) featureProvider, apiRequestContext.getMediaType(), id, featureId, requestBody);
    }

    @Path("/{id}/items/{featureid}")
    @DELETE
    public Response deleteItem(@Auth Optional<User> optionalUser, @Context OgcApi service,
                               @PathParam("id") String id, @PathParam("featureid") final String featureId) {

        FeatureProvider2 featureProvider = providers.getFeatureProviderOrThrow(service.getData(), service.getData().getCollections().get(id));

        checkTransactional(featureProvider);

        checkAuthorization(service.getData(), optionalUser);

        return commandHandler.deleteItemResponse((FeatureTransactions) featureProvider, id, featureId);
    }

    private void checkTransactional(FeatureProvider2 featureProvider) {
        if (!featureProvider.supportsTransactions()) {
            throw new NotAllowedException("GET");
        }
    }
}
