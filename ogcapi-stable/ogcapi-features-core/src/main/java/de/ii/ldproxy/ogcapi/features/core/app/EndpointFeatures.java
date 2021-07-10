/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition.Builder;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.ParameterExtension;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.domain.PropertyTransformation;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreValidation;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutableQueryInputFeature;
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutableQueryInputFeatures;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorOpenApi;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.models.media.Schema;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Provides
@Instantiate
public class EndpointFeatures extends EndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointFeatures.class);
    private static final List<String> TAGS = ImmutableList.of("Access data");

    private final SchemaGeneratorOpenApi schemaGeneratorFeature;
    private final EntityRegistry entityRegistry;
    private final FeaturesCoreProviders providers;
    private final FeaturesQuery ogcApiFeaturesQuery;
    private final FeaturesCoreQueriesHandler queryHandler;
    private final FeaturesCoreValidation featuresCoreValidator;

    public EndpointFeatures(@Requires ExtensionRegistry extensionRegistry,
                            @Requires EntityRegistry entityRegistry,
                            @Requires FeaturesCoreProviders providers,
                            @Requires FeaturesQuery ogcApiFeaturesQuery,
                            @Requires FeaturesCoreQueriesHandler queryHandler,
                            @Requires FeaturesCoreValidation featuresCoreValidator,
                            @Requires SchemaGeneratorOpenApi schemaGeneratorFeature) {
        super(extensionRegistry);
        this.entityRegistry = entityRegistry;
        this.providers = providers;
        this.ogcApiFeaturesQuery = ogcApiFeaturesQuery;
        this.queryHandler = queryHandler;
        this.featuresCoreValidator = featuresCoreValidator;
        this.schemaGeneratorFeature = schemaGeneratorFeature;
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
    public ValidationResult onStartup(OgcApiDataV2 apiData, MODE apiValidation) {
        ValidationResult result = super.onStartup(apiData, apiValidation);

        // no additional operational checks for now, only validation; we can stop, if no validation is requested
        if (apiValidation== MODE.NONE)
            return result;

        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
                .from(result)
                .mode(apiValidation);

        Map<String, FeatureSchema> featureSchemas = providers.getFeatureSchemas(apiData);

        List<String> invalidCollections = featuresCoreValidator.getCollectionsWithoutType(apiData, featureSchemas);
        for (String invalidCollection : invalidCollections) {
            builder.addStrictErrors(MessageFormat.format("The Collection ''{0}'' is invalid, because its feature type was not found in the provider schema.", invalidCollection));
        }

        // get Features Core configurations to process
        Map<String, FeaturesCoreConfiguration> coreConfigs = apiData.getCollections()
                                                                 .entrySet()
                                                                 .stream()
                                                                 .map(entry -> {
                                                                     final FeatureTypeConfigurationOgcApi collectionData = entry.getValue();
                                                                     final FeaturesCoreConfiguration config = collectionData.getExtension(FeaturesCoreConfiguration.class).orElse(null);
                                                                     if (Objects.isNull(config))
                                                                         return null;
                                                                     return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), config);
                                                                 })
                                                                 .filter(Objects::nonNull)
                                                                 .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, Collection<String>> transformationKeys = coreConfigs.entrySet()
                                                   .stream()
                                                   .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()
                                                                                                                                      .getTransformations()
                                                                                                                                      .keySet()))
                                                   .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
        for (Map.Entry<String, Collection<String>> stringCollectionEntry : featuresCoreValidator.getInvalidPropertyKeys(transformationKeys, featureSchemas).entrySet()) {
            for (String property : stringCollectionEntry.getValue()) {
                builder.addStrictErrors(MessageFormat.format("A transformation for property ''{0}'' in collection ''{1}'' is invalid, because the property was not found in the provider schema.", property, stringCollectionEntry.getKey()));
            }
        }

        transformationKeys = coreConfigs.entrySet()
                          .stream()
                          .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()
                                                                                                    .getQueryables()
                                                                                                    .orElse(FeaturesCollectionQueryables.of())
                                                                                                    .getAll()))
                          .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Map.Entry<String, Collection<String>> stringCollectionEntry : featuresCoreValidator.getInvalidPropertyKeys(transformationKeys, featureSchemas).entrySet()) {
            for (String property : stringCollectionEntry.getValue()) {
                builder.addStrictErrors(MessageFormat.format("A queryable ''{0}'' in collection ''{1}'' is invalid, because the property was not found in the provider schema.", property, stringCollectionEntry.getKey()));
            }
        }

        for (Map.Entry<String,FeaturesCoreConfiguration> entry : coreConfigs.entrySet()) {
            String collectionId = entry.getKey();
            FeaturesCoreConfiguration config = entry.getValue();
            if (config.getMinimumPageSize()<1) {
                builder.addStrictErrors(MessageFormat.format("The minimum page size ''{0}'' in collection ''{1}'' is invalid, it must be a positive integer.", config.getMinimumPageSize(), collectionId));
            }
            if (config.getMinimumPageSize()>config.getMaximumPageSize()) {
                builder.addStrictErrors(MessageFormat.format("The minimum page size ''{0}'' in collection ''{1}'' is invalid, it cannot be greater than the maximum page size ''{2}''.", config.getMinimumPageSize(), collectionId, config.getMaximumPageSize()));
            }
            if (config.getMinimumPageSize()>config.getDefaultPageSize()) {
                builder.addStrictErrors(MessageFormat.format("The minimum page size ''{0}'' in collection ''{1}'' is invalid, it cannot be greater than the default page size ''{2}''.", config.getMinimumPageSize(), collectionId, config.getDefaultPageSize()));
            }
            if (config.getMaximumPageSize()<config.getDefaultPageSize()) {
                builder.addStrictErrors(MessageFormat.format("The maxmimum page size ''{0}'' in collection ''{1}'' is invalid, it must be at least the default page size ''{2}''.", config.getMaximumPageSize(), collectionId, config.getDefaultPageSize()));
            }
        }

        Set<String> codelists = entityRegistry.getEntitiesForType(Codelist.class)
                                              .stream()
                                              .map(Codelist::getId)
                                              .collect(Collectors.toUnmodifiableSet());
        for (Map.Entry<String, FeaturesCoreConfiguration> entry : coreConfigs.entrySet()) {
            String collectionId = entry.getKey();
            for (Map.Entry<String, PropertyTransformation> entry2 : entry.getValue().getTransformations().entrySet()) {
                String property = entry2.getKey();
                builder = entry2.getValue().validate(builder, collectionId, property, codelists);
            }
        }

        return builder.build();
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("collections")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_FEATURES);
        ImmutableList<OgcApiQueryParameter> allQueryParameters = extensionRegistry
            .getExtensionsForType(OgcApiQueryParameter.class)
            .stream()
            .sorted(Comparator.comparing(ParameterExtension::getName))
            .collect(ImmutableList.toImmutableList());

        generateDefinition(apiData, definitionBuilder, allQueryParameters, "/items",
            "retrieve features in the feature collection '",
            "The response is a document consisting of features in the collection. " +
                "The features included in the response are determined by the server based on the query parameters of the request. " +
                "To support access to larger collections without overloading the client, the API supports paged access with links " +
                "to the next page, if more features are selected that the page size. The `bbox` and `datetime` parameter can be " +
                "used to select only a subset of the features in the collection (the features that are in the bounding box or time interval). " +
                "The `bbox` parameter matches all features in the collection that are not associated with a location, too. " +
                "The `datetime` parameter matches all features in the collection that are not associated with a time stamp or interval, too. " +
                "The `limit` parameter may be used to control the subset of the selected features that should be returned in the response, " +
                "the page size. Each page may include information about the number of selected and returned features (`numberMatched` " +
                "and `numberReturned`) as well as links to support paging (link relation `next`).",
            "FEATURES");

        generateDefinition(apiData, definitionBuilder, allQueryParameters, "/items/{featureId}",
            "retrieve a feature in the feature collection '",
            "Fetch the feature with id `{featureId}`.", "FEATURE");

        return definitionBuilder.build();
    }

    private void generateDefinition(OgcApiDataV2 apiData, Builder definitionBuilder,
        ImmutableList<OgcApiQueryParameter> allQueryParameters, String subSubPath, String summary, String description,
        String logPrefix) {

        String path = "/collections/{collectionId}" + subSubPath;
        List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
        Optional<OgcApiPathParameter> optCollectionIdParam = pathParameters.stream()
            .filter(param -> param.getName().equals("collectionId")).findAny();

        if (optCollectionIdParam.isEmpty()) {
            LOGGER.error("Path parameter 'collectionId' is missing for resource at path '{}'. The resource will not be available.", path);
            return;
        }

        final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
        final boolean explode = collectionIdParam.getExplodeInOpenApi(apiData);

        if (explode) {
            for (String collectionId : collectionIdParam.getValues(apiData)) {
                Stream<OgcApiQueryParameter> queryParameters = allQueryParameters.stream()
                    .filter(qp -> qp.isApplicable(apiData, path, collectionId, HttpMethods.GET));

                generateCollectionDefinition(apiData, definitionBuilder, subSubPath, path,
                    pathParameters,
                    queryParameters, collectionId,
                    summary, description, logPrefix);

                // since the generation is quite expensive, check if the startup was interrupted
                // after every collection
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } else {
            Optional<String> representativeCollectionId = getRepresentativeCollectionId(apiData);
            Stream<OgcApiQueryParameter> queryParameters = allQueryParameters.stream();

            if (representativeCollectionId.isPresent()) {
                String collectionId = representativeCollectionId.get();
                queryParameters = allQueryParameters.stream()
                    .filter(qp -> qp.isApplicable(apiData, path, collectionId, HttpMethods.GET));
            }

            generateCollectionDefinition(apiData, definitionBuilder, subSubPath, path,
                pathParameters,
                queryParameters, "{collectionId}",
                summary, description, logPrefix);
        }
    }

    private void generateCollectionDefinition(OgcApiDataV2 apiData, Builder definitionBuilder,
        String subSubPath, String path, List<OgcApiPathParameter> pathParameters,
        Stream<OgcApiQueryParameter> queryParameters, String collectionId,
        String summary, String description, String logPrefix) {

        final List<OgcApiQueryParameter> queryParameters1 = path.equals("/collections/{collectionId}/items")
            ? getQueryParametersWithQueryables(queryParameters, apiData, collectionId, logPrefix)
            : queryParameters.collect(Collectors.toList());
        final String operationSummary = summary + collectionId + "'";
        final Optional<String> operationDescription = Optional.of(description);
        String resourcePath = "/collections/" + collectionId + subSubPath;
        ImmutableOgcApiResourceData.Builder resourceBuilder = new ImmutableOgcApiResourceData.Builder()
            .path(resourcePath)
            .pathParameters(pathParameters);

        ApiOperation operation = addOperation(apiData, HttpMethods.GET, queryParameters1,
            collectionId, subSubPath, operationSummary, operationDescription, TAGS);

        if (operation != null)
            resourceBuilder.putOperations("GET", operation);

        definitionBuilder.putResources(resourcePath, resourceBuilder.build());
    }

    private List<OgcApiQueryParameter> getQueryParametersWithQueryables(
        Stream<OgcApiQueryParameter> generalList, OgcApiDataV2 apiData, String collectionId, String logPrefix) {

        Optional<FeaturesCoreConfiguration> coreConfiguration = apiData.getExtension(FeaturesCoreConfiguration.class, collectionId);
        final Map<String, String> filterableFields = coreConfiguration.map(FeaturesCoreConfiguration::getOtherFilterParameters)
            .orElse(ImmutableMap.of());

        Map<String, PropertyTransformation> transformations;
        if (coreConfiguration.isPresent()) {
            transformations = coreConfiguration.get().getTransformations();
            // TODO
        }

        List<OgcApiQueryParameter> build = Stream.concat(
            generalList,
            filterableFields.keySet().stream()
                .map(field -> {
                    Optional<Schema> schema2 = schemaGeneratorFeature.getSchemaOpenApi(apiData,
                        collectionId, field);
                    if (schema2.isEmpty()) {
                        LOGGER.warn(
                            "Query parameter for property '{}' at path '/collections/{}/items' could not be created, the property was not found in the feature schema.",
                            field,
                            collectionId);
                        return null;
                    }
                    return new ImmutableQueryParameterTemplateQueryable.Builder()
                        .apiId(apiData.getId())
                        .collectionId(collectionId)
                        .name(field)
                        .description("Filter the collection by property '" + field + "'")
                        .schema(schema2.get())
                        .build();
                })
                .filter(Objects::nonNull))
            .collect(Collectors.toList());

        return build;
    }

    @GET
    @Path("/{collectionId}/items")
    public Response getItems(@Auth Optional<User> optionalUser,
                             @Context OgcApi api,
                             @Context ApiRequestContext requestContext,
                             @Context UriInfo uriInfo,
                             @PathParam("collectionId") String collectionId) {
        checkCollectionExists(api.getData(), collectionId);

        FeatureTypeConfigurationOgcApi collectionData = api.getData()
                                                       .getCollections()
                                                       .get(collectionId);

        FeaturesCoreConfiguration coreConfiguration = collectionData.getExtension(FeaturesCoreConfiguration.class)
                                                                    .filter(ExtensionConfiguration::isEnabled)
                                                                    .filter(cfg -> cfg.getItemType().orElse(FeaturesCoreConfiguration.ItemType.feature) != FeaturesCoreConfiguration.ItemType.unknown)
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
                .featureProvider(providers.getFeatureProviderOrThrow(api.getData(), collectionData))
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
        checkCollectionExists(api.getData(), collectionId);

        FeatureTypeConfigurationOgcApi collectionData = api.getData()
                                                           .getCollections()
                                                           .get(collectionId);

        FeaturesCoreConfiguration coreConfiguration = collectionData.getExtension(FeaturesCoreConfiguration.class)
                                                                    .filter(ExtensionConfiguration::isEnabled)
                                                                    .filter(cfg -> cfg.getItemType().orElse(FeaturesCoreConfiguration.ItemType.feature) != FeaturesCoreConfiguration.ItemType.unknown)
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
                .featureProvider(providers.getFeatureProviderOrThrow(api.getData(), collectionData))
                .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
                .includeLinkHeader(includeLinkHeader)
                .build();

        return queryHandler.handle(FeaturesCoreQueriesHandlerImpl.Query.FEATURE, queryInput, requestContext);
    }
}
