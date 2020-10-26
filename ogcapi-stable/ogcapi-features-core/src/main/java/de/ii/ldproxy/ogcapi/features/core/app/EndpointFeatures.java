/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableQueryParameterTemplateQueryable;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.FoundationConfiguration;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTypeMapping2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutableQueryInputFeature;
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutableQueryInputFeatures;
import de.ii.xtraplatform.auth.domain.User;
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
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class EndpointFeatures extends EndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointFeatures.class);
    private static final List<String> TAGS = ImmutableList.of("Access data");

    private final FeaturesCoreProviders providers;
    private final FeaturesQuery ogcApiFeaturesQuery;
    private final FeaturesCoreQueriesHandler queryHandler;

    public EndpointFeatures(@Requires ExtensionRegistry extensionRegistry,
                            @Requires FeaturesCoreProviders providers,
                            @Requires FeaturesQuery ogcApiFeaturesQuery,
                            @Requires FeaturesCoreQueriesHandler queryHandler) {
        super(extensionRegistry);
        this.providers = providers;
        this.ogcApiFeaturesQuery = ogcApiFeaturesQuery;
        this.queryHandler = queryHandler;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(FeatureFormatExtension.class);
        return formats;
    }

    @Override
    public ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        int apiDataHash = apiData.hashCode();
        if (!apiDefinitions.containsKey(apiDataHash)) {
            LOGGER.info("OAPI GEN");
            ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                    .apiEntrypoint("collections")
                    .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_FEATURES);
            String subSubPath = "/items";
            String path = "/collections/{collectionId}" + subSubPath;
            List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
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
                    final List<OgcApiQueryParameter> queryParameters = explode ?
                            getQueryParameters(extensionRegistry, apiData, path, collectionId) :
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
                    ApiOperation operation = addOperation(apiData, HttpMethods.GET, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, TAGS);
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
                    final List<OgcApiQueryParameter> queryParameters = explode ?
                            getQueryParameters(extensionRegistry, apiData, path, collectionId) :
                            getQueryParameters(extensionRegistry, apiData, path);
                    final String operationSummary = "retrieve a feature in the feature collection '" + collectionId + "'";
                    final Optional<String> operationDescription = Optional.of("Fetch the feature with id `{featureId}`.");
                    String resourcePath = "/collections/" + collectionId + subSubPath;
                    ImmutableOgcApiResourceData.Builder resourceBuilder = new ImmutableOgcApiResourceData.Builder()
                            .path(resourcePath)
                            .pathParameters(pathParameters);
                    ApiOperation operation = addOperation(apiData, HttpMethods.GET, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, TAGS);
                    if (operation!=null)
                        resourceBuilder.putOperations("GET", operation);
                    definitionBuilder.putResources(resourcePath, resourceBuilder.build());
                }

            }
            apiDefinitions.put(apiDataHash, definitionBuilder.build());
        }

        return apiDefinitions.get(apiDataHash);
    }

    @Override
    public ImmutableList<OgcApiQueryParameter> getQueryParameters(ExtensionRegistry extensionRegistry, OgcApiDataV2 apiData, String definitionPath, String collectionId) {
        ImmutableList<OgcApiQueryParameter> generalList = super.getQueryParameters(extensionRegistry, apiData, definitionPath, collectionId);

        FeatureTypeConfigurationOgcApi featureType = apiData.getCollections().get(collectionId);
        Optional<FeaturesCoreConfiguration> coreConfiguration = featureType==null ?
                apiData.getExtension(FeaturesCoreConfiguration.class) :
                featureType.getExtension(FeaturesCoreConfiguration.class);

        final Map<String, String> filterableFields = coreConfiguration.map(FeaturesCoreConfiguration::getOtherFilterParameters)
                .orElse(ImmutableMap.of());

        Map<String, FeatureTypeMapping2> transformations;
        if (coreConfiguration.isPresent()) {
            transformations = coreConfiguration.get().getTransformations();
            // TODO
        }

        if (definitionPath.equals("/collections/{collectionId}/items"))
            return ImmutableList.<OgcApiQueryParameter>builder()
                    .addAll(generalList)
                    .addAll(filterableFields.keySet().stream()
                        .map(field -> new ImmutableQueryParameterTemplateQueryable.Builder()
                                    .apiId(apiData.getId())
                                    .collectionId(collectionId)
                                    .name(field)
                                    .description("Filter the collection by property '" + field + "'")
                                    .schema(new StringSchema())
                                    .build())
                        .collect(Collectors.toList()))
                    .build();

        return generalList;
    }

    @GET
    @Path("/{collectionId}/items")
    public Response getItems(@Auth Optional<User> optionalUser,
                             @Context OgcApi api,
                             @Context ApiRequestContext requestContext,
                             @Context UriInfo uriInfo,
                             @PathParam("collectionId") String collectionId) {
        checkAuthorization(api.getData(), optionalUser);
        checkCollectionExists(api.getData(), collectionId);

        FeatureTypeConfigurationOgcApi collectionData = api.getData()
                                                       .getCollections()
                                                       .get(collectionId);

        FeaturesCoreConfiguration coreConfiguration = collectionData.getExtension(FeaturesCoreConfiguration.class)
                                                                    .orElseThrow(() -> new NotFoundException(MessageFormat.format("Features are not supported in API ''{0}'', collection ''{1}''.", api.getId(), collectionId)));

        int minimumPageSize = coreConfiguration.getMinimumPageSize();
        int defaultPageSize = coreConfiguration.getDefaultPageSize();
        int maxPageSize = coreConfiguration.getMaximumPageSize();
        boolean showsFeatureSelfLink = coreConfiguration.getShowsFeatureSelfLink();
        boolean includeLinkHeader = api.getData().getExtension(FoundationConfiguration.class)
                .map(FoundationConfiguration::getIncludeLinkHeader)
                .orElse(false);

        List<OgcApiQueryParameter> allowedParameters = getQueryParameters(extensionRegistry, api.getData(), "/collections/{collectionId}/items", collectionId);
        FeatureQuery query = ogcApiFeaturesQuery.requestToFeatureQuery(api.getData(), collectionData, coreConfiguration, minimumPageSize, defaultPageSize, maxPageSize, toFlatMap(uriInfo.getQueryParameters()), allowedParameters);

        FeaturesCoreQueriesHandler.QueryInputFeatures queryInput = new ImmutableQueryInputFeatures.Builder()
                .collectionId(collectionId)
                .query(query)
                .featureProvider(providers.getFeatureProvider(api.getData(), collectionData))
                .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
                .defaultPageSize(Optional.of(defaultPageSize))
                .showsFeatureSelfLink(showsFeatureSelfLink)
                .includeLinkHeader(includeLinkHeader)
                .build();

        return queryHandler.handle(FeaturesCoreQueriesHandlerImpl.Query.FEATURES, queryInput, requestContext);
    }

    @GET
    @Path("/{collectionId}/items/{featureId}")
    public Response getItem(@Auth Optional<User> optionalUser,
                            @Context OgcApi api,
                            @Context ApiRequestContext requestContext,
                            @Context UriInfo uriInfo,
                            @PathParam("collectionId") String collectionId,
                            @PathParam("featureId") String featureId) {
        checkAuthorization(api.getData(), optionalUser);
        checkCollectionExists(api.getData(), collectionId);

        FeatureTypeConfigurationOgcApi collectionData = api.getData()
                                                           .getCollections()
                                                           .get(collectionId);

        FeaturesCoreConfiguration coreConfiguration = collectionData.getExtension(FeaturesCoreConfiguration.class)
                                                                    .orElseThrow(() -> new NotFoundException("Features are not supported for this API."));

        boolean includeLinkHeader = api.getData().getExtension(FoundationConfiguration.class)
                .map(FoundationConfiguration::getIncludeLinkHeader)
                .orElse(false);

        List<OgcApiQueryParameter> allowedParameters = getQueryParameters(extensionRegistry, api.getData(), "/collections/{collectionId}/items/{featureId}", collectionId);
        FeatureQuery query = ogcApiFeaturesQuery.requestToFeatureQuery(api.getData(), collectionData, coreConfiguration, toFlatMap(uriInfo.getQueryParameters()), allowedParameters, featureId);

        FeaturesCoreQueriesHandler.QueryInputFeature queryInput = new ImmutableQueryInputFeature.Builder()
                .collectionId(collectionId)
                .featureId(featureId)
                .query(query)
                .featureProvider(providers.getFeatureProvider(api.getData(), collectionData))
                .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
                .includeLinkHeader(includeLinkHeader)
                .build();

        return queryHandler.handle(FeaturesCoreQueriesHandlerImpl.Query.FEATURE, queryInput, requestContext);
    }
}
