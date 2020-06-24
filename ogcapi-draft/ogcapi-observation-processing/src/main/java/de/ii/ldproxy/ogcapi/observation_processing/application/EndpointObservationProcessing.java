/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesQuery;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcess;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessChain;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.observation_processing.api.ImmutableOgcApiQueryInputObservationProcessing;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingOutputFormat;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingQueriesHandler;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;

@Component
@Provides
@Instantiate
public class EndpointObservationProcessing extends OgcApiEndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointObservationProcessing.class);
    private static final List<String> TAGS = ImmutableList.of("DAPA"); // TODO make configurable
    private static final String DAPA_PATH_ELEMENT = "dapa";

    final OgcApiFeatureCoreProviders providers;
    final OgcApiFeaturesQuery ogcApiFeaturesQuery;
    final ObservationProcessingQueriesHandler queryHandler;
    final FeatureProcessInfo featureProcessInfo;

    public EndpointObservationProcessing(@Requires OgcApiExtensionRegistry extensionRegistry,
                                         @Requires OgcApiFeatureCoreProviders providers,
                                         @Requires OgcApiFeaturesQuery ogcApiFeaturesQuery,
                                         @Requires ObservationProcessingQueriesHandler queryHandler,
                                         @Requires FeatureProcessInfo featureProcessInfo) {
        super(extensionRegistry);
        this.providers = providers;
        this.ogcApiFeaturesQuery = ogcApiFeaturesQuery;
        this.queryHandler = queryHandler;
        this.featureProcessInfo = featureProcessInfo;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class);
    }

    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(ObservationProcessingOutputFormat.class);
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
                    .sortPriority(10020);
            featureProcessInfo.getProcessingChains(apiData, ObservationProcess.class)
                    .stream()
                    .forEach(chain -> {
                        final String subSubPath = chain.getSubSubPath();
                        final String path = "/collections/{collectionId}" + subSubPath;
                        final Set<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
                        final Set<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
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
                                    String operationSummary = chain.getOperationSummary();
                                    Optional<String> operationDescription = chain.getOperationDescription();
                                    Optional<String> responseDescription = chain.getResponseDescription();
                                    List<String> tags;
                                    switch (subSubPath) {
                                        case "/"+DAPA_PATH_ELEMENT+"/position":
                                            operationSummary = "retrieve information about observations at a position";
                                            operationDescription = Optional.of("A point observation feature with a point geometry at the selected location (`coord`) " +
                                                    "at the selected time or for each time step in the selected time interval (`datetime`). " +
                                                    "The feature contains a property for each selected variable (`variables`) for which " +
                                                    "a value can be interpolated. " +
                                                    "The time steps are determined from the information in the original data.");
                                            break;

                                        case "/"+DAPA_PATH_ELEMENT+"/position:aggregate-time":
                                            operationSummary = "retrieve information about observations at a position and compute values aggregated over time";
                                            operationDescription = Optional.of("A point observation feature with a point geometry at the selected location (`coord`). " +
                                                        "The feature includes a property for each combination of variables (`variables`) for which " +
                                                        "a value can be interpolated and the statistical functions (`functions`). " +
                                                        "The property value is the function applied to the interpolated values for each time step " +
                                                        "in the selected time interval (`datetime`).");
                                            break;

                                        case "/"+DAPA_PATH_ELEMENT+"/area":
                                            operationSummary = "retrieve information about observations in an area";
                                            operationDescription = Optional.of("A collection of point features, one for each location in the " +
                                                "selected area (`coord` or `bbox`), at the selected time or for each time step in the " +
                                                "selected time interval (`datetime`). " +
                                                "The time steps are determined from the information in the original data. " +
                                                "The feature contains a property for each selected variable (`variables`) for which " +
                                                "an observation is available.");
                                            break;

                                        case "/"+DAPA_PATH_ELEMENT+"/area:aggregate-time":
                                            operationSummary = "retrieve information about observations in an area and compute values aggregated over time";
                                            operationDescription = Optional.of("A collection of point features, one for each location in the " +
                                                "selected area (`coord` or `bbox`). " +
                                                "Each feature includes a property for each combination of variables (`variables`) for which " +
                                                "a value can be interpolated and the statistical functions (`functions`). " +
                                                "The property value is the function applied to the observed values at the location " +
                                                "for each time step in the selected time interval (`datetime`).");
                                            break;

                                        case "/"+DAPA_PATH_ELEMENT+"/area:aggregate-space":
                                            operationSummary = "retrieve information about observations in an area and compute values aggregated over space";
                                            operationDescription = Optional.of("A feature with the (multi-)polygon geometry of the selected area (`coord` or `bbox`) " +
                                                "at the selected time or for each time step in the selected time interval (`datetime`). " +
                                                "The time steps are determined from the information in the original data. " +
                                                "Each feature includes a property for each combination of variables (`variables`) for which " +
                                                "a value can be interpolated and the statistical functions (`functions`). " +
                                                "The property value is the function applied to all observed values in the area " +
                                                "for each time step.");
                                            break;

                                        case "/"+DAPA_PATH_ELEMENT+"/area:aggregate-space-time":
                                            operationSummary = "retrieve information about observations in an area and compute values aggregated over space and time";
                                            operationDescription = Optional.of("A feature with the (multi-)polygon geometry of the selected area (`coord` or `bbox`). " +
                                                "Each feature includes a property for each combination of variables (`variables`) for which " +
                                                "a value can be interpolated and the statistical functions (`functions`). " +
                                                "The property value is the function applied to all observed values in the area " +
                                                "over all time steps.");
                                            break;

                                        case "/"+DAPA_PATH_ELEMENT+"/resample-to-grid":
                                            operationSummary = "retrieve information about observations in an area, resampled to a regular grid";
                                            operationDescription = Optional.of("A collection of point features in a rectangular area (`bbox`), at the " +
                                                "selected time or for each time step in the selected time interval (`datetime`). " +
                                                "The time steps are determined from the information in the original data. " +
                                                "The result of the parent resource is taken and resampled to a grid and each point feature " +
                                                "represents a cell of the grid. " +
                                                "The grid size is determined by the parameters `width` and `height`. If only `width` " +
                                                "is provided, the value of `height` is derived from the area." +
                                                "Each feature contains a property for each selected variable (`variables`) for which " +
                                                "a value can be interpolated.");
                                            break;

                                        case "/"+DAPA_PATH_ELEMENT+"/resample-to-grid:aggregate-time":
                                            operationSummary = "retrieve information about observations in an area, resampled to a regular grid and aggregated over time";
                                            operationDescription = Optional.of("A collection of point features in a rectangular area (`bbox`). " +
                                                "The result of the parent resource is taken and resampled to a grid and each point feature " +
                                                "represents a cell of the grid. " +
                                                "The grid size is determined by the parameters `width` and `height`. If only `width` " +
                                                "is provided, the value of `height` is derived from the area." +
                                                "Each feature includes a property for each combination of variables (`variables`) for which " +
                                                "a value can be interpolated and the statistical functions (`functions`). " +
                                                "The property value is the function applied to the interpolated values " +
                                                "for each time step in the selected time interval (`datetime`).");
                                            break;
                                    }
                                    String resourcePath = "/collections/" + collectionId + subSubPath;
                                    ImmutableOgcApiResourceProcess.Builder resourceBuilder = new ImmutableOgcApiResourceProcess.Builder()
                                            .path(resourcePath)
                                            .pathParameters(pathParameters);
                                    OgcApiOperation operation = addOperation(apiData, OgcApiContext.HttpMethods.GET, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, TAGS);
                                    if (operation!=null)
                                        resourceBuilder.putOperations("GET", operation);
                                    definitionBuilder.putResources(resourcePath, resourceBuilder.build());
                                });
                    });

            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
    }

    @GET
    @Path("/{collectionId}/"+DAPA_PATH_ELEMENT+"/{processIds}")
    @Produces("application/geo+json")
    public Response getProcessResult(@Auth Optional<User> optionalUser,
                                     @Context OgcApiApi api,
                                     @Context OgcApiRequestContext requestContext,
                                     @Context UriInfo uriInfo,
                                     @PathParam("collectionId") String collectionId,
                                     @PathParam("processIds") String processIds) {
        checkAuthorization(api.getData(), optionalUser);
        FeatureProcessChain processChain = featureProcessInfo.getProcessingChains(api.getData(), collectionId, ObservationProcess.class).stream()
                .filter(chain -> chain.getSubSubPath().equals("/"+DAPA_PATH_ELEMENT+"/"+processIds))
                .findAny()
                .orElseThrow(() -> new NotFoundException());
        final String path = "/collections/{collectionId}/"+DAPA_PATH_ELEMENT+"/"+processIds;
        checkPathParameter(extensionRegistry, api.getData(), path, "collectionId", collectionId);
        final Set<OgcApiQueryParameter> allowedParameters = getQueryParameters(extensionRegistry, api.getData(), path, collectionId);
        return getResponse(optionalUser, api.getData(), requestContext, uriInfo, collectionId, allowedParameters, processChain);
    }

    Response getResponse(Optional<User> optionalUser, OgcApiApiDataV2 apiData, OgcApiRequestContext requestContext,
                         UriInfo uriInfo, String collectionId, Set<OgcApiQueryParameter> allowedParameters, FeatureProcessChain processChain) {

        // TODO check that the request is not considered to be too demanding

        final FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);
        final OgcApiFeaturesCoreConfiguration coreConfiguration = getExtensionConfiguration(apiData, collectionData, OgcApiFeaturesCoreConfiguration.class).orElseThrow(NotFoundException::new);
        final int minimumPageSize = coreConfiguration.getMinimumPageSize();
        final int defaultPageSize = coreConfiguration.getDefaultPageSize();
        final int maxPageSize = coreConfiguration.getMaxPageSize();
        final boolean includeLinkHeader = getExtensionConfiguration(apiData, OgcApiCommonConfiguration.class)
                                                .map(OgcApiCommonConfiguration::getIncludeLinkHeader)
                                                .orElse(false);
        Map<String, String> queryParams = toFlatMap(uriInfo.getQueryParameters());

        // first execute the information that is passed as processing parameters
        Map<String, Object> processingParameters = new HashMap<>();
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

        List<Variable> variables = getExtensionConfiguration(apiData, ObservationProcessingConfiguration.class)
                .map(ObservationProcessingConfiguration::getVariables)
                .orElse(ImmutableList.of());

        ObservationProcessingQueriesHandler.OgcApiQueryInputObservationProcessing queryInput = new ImmutableOgcApiQueryInputObservationProcessing.Builder()
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
