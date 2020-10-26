/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.Example;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.ExternalDocumentation;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.FoundationConfiguration;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiResourceProcess;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcess;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessChain;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.ProcessDocumentation;
import de.ii.ldproxy.ogcapi.observation_processing.api.DapaResultFormatExtension;
import de.ii.ldproxy.ogcapi.observation_processing.api.ImmutableQueryInputObservationProcessing;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingQueriesHandler;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import io.dropwizard.auth.Auth;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@Provides
@Instantiate
public class EndpointObservationProcessing extends EndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointObservationProcessing.class);
    private static final List<String> TAGS = ImmutableList.of("DAPA"); // TODO make configurable
    private static final String DAPA_PATH_ELEMENT = "dapa";

    final FeaturesCoreProviders providers;
    final FeaturesQuery ogcApiFeaturesQuery;
    final ObservationProcessingQueriesHandler queryHandler;
    final FeatureProcessInfo featureProcessInfo;

    public EndpointObservationProcessing(@Requires ExtensionRegistry extensionRegistry,
                                         @Requires FeaturesCoreProviders providers,
                                         @Requires FeaturesQuery ogcApiFeaturesQuery,
                                         @Requires ObservationProcessingQueriesHandler queryHandler,
                                         @Requires FeatureProcessInfo featureProcessInfo) {
        super(extensionRegistry);
        this.providers = providers;
        this.ogcApiFeaturesQuery = ogcApiFeaturesQuery;
        this.queryHandler = queryHandler;
        this.featureProcessInfo = featureProcessInfo;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return ObservationProcessingConfiguration.class;
    }

    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(DapaResultFormatExtension.class);
        return formats;
    }

    @Override
    public ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        int apiDataHash = apiData.hashCode();
        if (!apiDefinitions.containsKey(apiDataHash)) {
            ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                    .apiEntrypoint("collections")
                    .sortPriority(10020);
            featureProcessInfo.getProcessingChains(apiData, ObservationProcess.class)
                    .stream()
                    .forEach(chain -> {
                        final String subSubPath = chain.getSubSubPath();
                        final String path = "/collections/{collectionId}" + subSubPath;
                        final List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
                        final List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
                        final Optional<OgcApiPathParameter> optCollectionIdParam = pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
                        if (!optCollectionIdParam.isPresent()) {
                            LOGGER.error("Path parameter 'collectionId' missing for resource at path '" + path + "'. The GET method will not be available.");
                            return;
                        }

                        final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
                        boolean explode = collectionIdParam.getExplodeInOpenApi();
                        final Set<String> collectionIds = (explode) ?
                                collectionIdParam.getValues(apiData) :
                                ImmutableSet.of("{collectionId}");
                        collectionIds.stream()
                                .forEach(collectionId -> {
                                    if (explode) {
                                        if (!chain.asList().get(0).getSupportedCollections(apiData).contains(collectionId))
                                            // resources do not apply for this collection
                                            return;
                                    }

                                    FeatureTypeConfigurationOgcApi featureType = apiData.getCollections().get(collectionId);
                                    ObservationProcessingConfiguration config = featureType.getExtension(ObservationProcessingConfiguration.class)
                                                                                           .orElseThrow(() -> new RuntimeException("Could not retrieve Observation Process configuration."));
                                    Map<String, ProcessDocumentation> configDoc = config.getDocumentation();
                                    String operationSummary = chain.getOperationSummary();
                                    Optional<String> operationDescription = chain.getOperationDescription();
                                    Optional<String> responseDescription = chain.getResponseDescription();
                                    Optional<ExternalDocumentation> externalDocs = Optional.empty();
                                    Map<String, List<Example>> examples = ImmutableMap.of();
                                    List<String> tags;
                                    String processId = subSubPath.substring(DAPA_PATH_ELEMENT.length()+2);
                                    switch (processId) {
                                        case "position":
                                            operationSummary = configDoc.containsKey(processId) && configDoc.get(processId).getSummary().isPresent() ?
                                                    configDoc.get(processId).getSummary().get() :
                                                    "retrieve a time series for a position";
                                            operationDescription = configDoc.containsKey(processId) && configDoc.get(processId).getDescription().isPresent() ?
                                                    configDoc.get(processId).getDescription() :
                                                    Optional.of("Returns a time series at the selected location (parameter `coord` or `coordRef`) " +
                                                            "in the selected time interval or at the selected time instant (parameter `datetime`).\n\n" +
                                                            "The time series contains values for each selected variable (parameter `variables`) for which " +
                                                            "a value can be interpolated.\n\n" +
                                                            "The time steps are determined from the information in the original data.");
                                            break;

                                        case "position:aggregate-time":
                                            operationSummary = configDoc.containsKey(processId) && configDoc.get(processId).getSummary().isPresent() ?
                                                    configDoc.get(processId).getSummary().get() :
                                                    "retrieve aggregated observation values for a position, aggregated over time";
                                            operationDescription = configDoc.containsKey(processId) && configDoc.get(processId).getDescription().isPresent() ?
                                                    configDoc.get(processId).getDescription() :
                                                    Optional.of("Returns observation values at the selected location (parameter `coord` or `coordRef`) " +
                                                            "in the selected time interval or at the selected time instant (parameter `datetime`).\n\n" +
                                                            "All values in the time interval for each requested variable (parameter `variables`) are aggregated " +
                                                            "and each of the requested statistical functions (parameter `functions`) is applied to " +
                                                            "the aggregated values.");
                                            break;

                                        case "area":
                                            operationSummary = configDoc.containsKey(processId) && configDoc.get(processId).getSummary().isPresent() ?
                                                    configDoc.get(processId).getSummary().get() :
                                                    "retrieve a time series for each station in an area";
                                            operationDescription = configDoc.containsKey(processId) && configDoc.get(processId).getDescription().isPresent() ?
                                                    configDoc.get(processId).getDescription() :
                                                    Optional.of("Returns a time series for each station in an area (parameter `box`, `coord` or `coordRef`) " +
                                                            "in the selected time interval or at the selected time instant (parameter `datetime`).\n\n" +
                                                            "Each time series contains values for each selected variable (parameter `variables`) for which " +
                                                            "a value has been observed at the station during the time interval.\n\n" +
                                                            "The time steps are determined from the information in the original data.");
                                            break;

                                        case "area:aggregate-time":
                                            operationSummary = configDoc.containsKey(processId) && configDoc.get(processId).getSummary().isPresent() ?
                                                    configDoc.get(processId).getSummary().get() :
                                                    "retrieve aggregated observation values for each station in an area, aggregated over time";
                                            operationDescription = configDoc.containsKey(processId) && configDoc.get(processId).getDescription().isPresent() ?
                                                    configDoc.get(processId).getDescription() :
                                                    Optional.of("Returns observation values for each station in an area (parameter `box`, `coord` or `coordRef`) " +
                                                            "in the selected time interval or at the selected time instant (parameter `datetime`).\n\n" +
                                                            "All values of each station in the time interval for each requested variable (parameter `variables`) are aggregated " +
                                                            "and each of the requested statistical functions (parameter `functions`) is applied to " +
                                                            "the aggregated values.");
                                            break;

                                        case "area:aggregate-space":
                                            operationSummary = configDoc.containsKey(processId) && configDoc.get(processId).getSummary().isPresent() ?
                                                    configDoc.get(processId).getSummary().get() :
                                                    "retrieve a time series for an area, aggregated over all stations in the area";
                                            operationDescription = configDoc.containsKey(processId) && configDoc.get(processId).getDescription().isPresent() ?
                                                    configDoc.get(processId).getDescription() :
                                                    Optional.of("Returns a time series for an area (parameter `bbox`, `coord` or `coordRef`) " +
                                                            "in the selected time interval or at the selected time instant (parameter `datetime`).\n\n" +
                                                            "All values in the area for each requested variable (parameter `variables`) are aggregated " +
                                                            "for each time step and each of the requested statistical functions (parameter `functions`) " +
                                                            "is applied to the aggregated values.");
                                            break;

                                        case "area:aggregate-space-time":
                                            operationSummary = configDoc.containsKey(processId) && configDoc.get(processId).getSummary().isPresent() ?
                                                    configDoc.get(processId).getSummary().get() :
                                                    "retrieve aggregated observation values for an area, aggregated over space and time";
                                            operationDescription = configDoc.containsKey(processId) && configDoc.get(processId).getDescription().isPresent() ?
                                                    configDoc.get(processId).getDescription() :
                                                    Optional.of("Returns observation values for an area (parameter `bbox`, `coord` or `coordRef`) " +
                                                            "in the selected time interval or at the selected time instant (parameter `datetime`).\n\n" +
                                                            "All values for each requested variable (parameter `variables`) are aggregated " +
                                                            "and each of the requested statistical functions (parameter `functions`) is applied to " +
                                                            "the aggregated values.");
                                            break;

                                        case "resample-to-grid":
                                            operationSummary = configDoc.containsKey(processId) && configDoc.get(processId).getSummary().isPresent() ?
                                                    configDoc.get(processId).getSummary().get() :
                                                    "retrieve observations in a spatio-temporal cube";
                                            operationDescription = configDoc.containsKey(processId) && configDoc.get(processId).getDescription().isPresent() ?
                                                    configDoc.get(processId).getDescription() :
                                                    Optional.of("Retrieves observation values for each cell in a spatio-temporal cube consisting of a rectangular " +
                                                            "spatial grid (parameter `box` or `coordRef`) and the time steps in a time interval (parameter `datetime`). " +
                                                            "The time steps are determined from the information in the original data.\n\n" +
                                                            "The cells of the spatial grid are determined by the parameters `width` and `height`. If only `width` " +
                                                            "is provided, the value of `height` is derived from the area.\n\n" +
                                                            "Each cell contains values for each selected variable (parameter `variables`) for which " +
                                                            "a value could be interpolated from the observations.");
                                            break;

                                        case "resample-to-grid:aggregate-time":
                                            operationSummary = configDoc.containsKey(processId) && configDoc.get(processId).getSummary().isPresent() ?
                                                    configDoc.get(processId).getSummary().get() :
                                                    "retrieve aggregated observations in a spatial grid, aggregated over time";
                                            operationDescription = configDoc.containsKey(processId) && configDoc.get(processId).getDescription().isPresent() ?
                                                    configDoc.get(processId).getDescription() :
                                                    Optional.of("Retrieves observation values for each cell in a rectangular spatial grid (parameter `box` or `coordRef`) " +
                                                            "in the selected time interval or at the selected time instant (parameter `datetime`).\n\n" +
                                                            "The cells of the spatial grid are determined by the parameters `width` and `height`. If only `width` " +
                                                            "is provided, the value of `height` is derived from the area.\n\n" +
                                                            "For each cell, all values in the time interval for each requested variable (parameter `variables`) are aggregated " +
                                                            "and each of the requested statistical functions (parameter `functions`) is applied to " +
                                                            "the aggregated values.");
                                            break;
                                    }
                                    externalDocs = configDoc.containsKey(processId) ? configDoc.get(processId).getExternalDocs() : Optional.empty();
                                    examples = configDoc.containsKey(processId) ? configDoc.get(processId).getExamples() : ImmutableMap.of();
                                    String resourcePath = "/collections/" + collectionId + subSubPath;
                                    ImmutableOgcApiResourceProcess.Builder resourceBuilder = new ImmutableOgcApiResourceProcess.Builder()
                                            .path(resourcePath)
                                            .pathParameters(pathParameters);
                                    ApiOperation operation = addOperation(apiData, HttpMethods.GET, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, externalDocs, examples, TAGS);
                                    if (operation!=null)
                                        resourceBuilder.putOperations("GET", operation);
                                    definitionBuilder.putResources(resourcePath, resourceBuilder.build());
                                });
                    });

            apiDefinitions.put(apiDataHash, definitionBuilder.build());
        }

        return apiDefinitions.get(apiDataHash);
    }

    @GET
    @Path("/{collectionId}/"+DAPA_PATH_ELEMENT+"/{processIds}")
    public Response getProcessResult(@Auth Optional<User> optionalUser,
                                     @Context OgcApi api,
                                     @Context ApiRequestContext requestContext,
                                     @Context UriInfo uriInfo,
                                     @PathParam("collectionId") String collectionId,
                                     @PathParam("processIds") String processIds) {
        checkAuthorization(api.getData(), optionalUser);
        FeatureProcessChain processChain = featureProcessInfo.getProcessingChains(api.getData(), collectionId, ObservationProcess.class).stream()
                .filter(chain -> chain.getSubSubPath().equals("/"+DAPA_PATH_ELEMENT+"/"+processIds))
                .findAny()
                .orElseThrow(() -> new NotFoundException("The requested path is not a resource in this API."));
        final String path = "/collections/{collectionId}/"+DAPA_PATH_ELEMENT+"/"+processIds;
        checkPathParameter(extensionRegistry, api.getData(), path, "collectionId", collectionId);
        final List<OgcApiQueryParameter> allowedParameters = getQueryParameters(extensionRegistry, api.getData(), path, collectionId);
        return getResponse(optionalUser, api.getData(), requestContext, uriInfo, collectionId, allowedParameters, processChain);
    }

    Response getResponse(Optional<User> optionalUser, OgcApiDataV2 apiData, ApiRequestContext requestContext,
                         UriInfo uriInfo, String collectionId, List<OgcApiQueryParameter> allowedParameters, FeatureProcessChain processChain) {

        // TODO check that the request is not considered to be too demanding

        final FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);
        final FeaturesCoreConfiguration coreConfiguration = collectionData.getExtension(FeaturesCoreConfiguration.class)
                                                                          .orElseThrow(() -> new NotFoundException(MessageFormat.format("Features are not supported in API ''{0}'', collection ''{1}''.", apiData.getId(), collectionId)));
        final int minimumPageSize = coreConfiguration.getMinimumPageSize();
        final int defaultPageSize = coreConfiguration.getDefaultPageSize();
        final int maxPageSize = coreConfiguration.getMaximumPageSize();
        final boolean includeLinkHeader = apiData.getExtension(FoundationConfiguration.class)
                                                .map(FoundationConfiguration::getIncludeLinkHeader)
                                                .orElse(false);
        Map<String, String> queryParams = toFlatMap(uriInfo.getQueryParameters());

        // first execute the information that is passed as processing parameters
        Map<String, Object> processingParameters = new HashMap<>();
        processingParameters.put("apiData", apiData);
        processingParameters.put("collectionId", collectionId);
        for (OgcApiQueryParameter parameter : allowedParameters) {
            processingParameters = parameter.transformContext(collectionData, processingParameters, queryParams, apiData);
        }

        // verify that the required input is available
        for (FeatureProcess featureProcess : processChain.asList()) {
            featureProcess.validateProcessingParameters(processingParameters);
        }
        // now execute the information to construct the feature query
        queryParams.put("limit", String.valueOf(maxPageSize));
        FeatureQuery query = ogcApiFeaturesQuery.requestToFeatureQuery(apiData, collectionData, coreConfiguration, minimumPageSize, defaultPageSize, maxPageSize, queryParams, allowedParameters);

        List<Variable> variables = apiData.getExtension(ObservationProcessingConfiguration.class)
                .map(ObservationProcessingConfiguration::getVariables)
                .orElse(ImmutableList.of());

        ObservationProcessingQueriesHandler.QueryInputObservationProcessing queryInput = new ImmutableQueryInputObservationProcessing.Builder()
                .featureProvider(providers.getFeatureProvider(apiData, collectionData))
                .collectionId(collectionId)
                .query(query)
                .variables(variables)
                .processes(processChain)
                .processingParameters(processingParameters)
                .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
                .includeLinkHeader(includeLinkHeader)
                .build();

        ObservationProcessingQueriesHandler.Query process = ObservationProcessingQueriesHandlerImpl.Query.PROCESS;
        return queryHandler.handle(process, queryInput, requestContext);
    }
}
