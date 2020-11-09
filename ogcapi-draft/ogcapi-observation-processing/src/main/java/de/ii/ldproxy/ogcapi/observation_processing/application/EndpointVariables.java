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
import de.ii.ldproxy.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.FoundationConfiguration;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiResourceProcess;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.observation_processing.api.DapaVariablesFormatExtension;
import de.ii.ldproxy.ogcapi.observation_processing.api.ImmutableQueryInputVariables;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingQueriesHandler;
import de.ii.xtraplatform.auth.domain.User;
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
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Provides
@Instantiate
public class EndpointVariables extends EndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointVariables.class);
    private static final String DAPA_PATH_ELEMENT = "dapa";
    private static final List<String> TAGS = ImmutableList.of("DAPA");

    private final ObservationProcessingQueriesHandler queryHandler;

    public EndpointVariables(@Requires ExtensionRegistry extensionRegistry,
                             @Requires ObservationProcessingQueriesHandler queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(DapaVariablesFormatExtension.class);
        return formats;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return ObservationProcessingConfiguration.class;
    }

    @Override
    public ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        int apiDataHash = apiData.hashCode();
        if (!apiDefinitions.containsKey(apiDataHash)) {
            ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                    .apiEntrypoint("collections")
                    .sortPriority(10010);
            final String subSubPath = "/"+ DAPA_PATH_ELEMENT +"/variables";
            final String path = "/collections/{collectionId}" + subSubPath;
            final List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
            final List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
            final Optional<OgcApiPathParameter> optCollectionIdParam = pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
            if (!optCollectionIdParam.isPresent()) {
                LOGGER.error("Path parameter 'collectionId' missing for resource at path '" + path + "'. The GET method will not be available.");
            } else {
                final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
                boolean explode = collectionIdParam.getExplodeInOpenApi();
                final Set<String> collectionIds = (explode) ?
                        collectionIdParam.getValues(apiData) :
                        ImmutableSet.of("{collectionId}");
                collectionIds.stream()
                        .forEach(collectionId -> {
                            String operationSummary = "fetch the observable properties included in this observation collection";
                            Optional<String> operationDescription = Optional.empty();
                            String resourcePath = "/collections/" + collectionId + subSubPath;
                            ImmutableOgcApiResourceProcess.Builder resourceBuilder = new ImmutableOgcApiResourceProcess.Builder()
                                    .path(resourcePath)
                                    .pathParameters(pathParameters);
                            ApiOperation operation = addOperation(apiData, HttpMethods.GET, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, TAGS);
                            if (operation!=null)
                                resourceBuilder.putOperations("GET", operation);
                            definitionBuilder.putResources(resourcePath, resourceBuilder.build());
                        });

            }
            apiDefinitions.put(apiDataHash, definitionBuilder.build());
        }

        return apiDefinitions.get(apiDataHash);
    }

    @GET
    @Path("/{collectionId}/"+ DAPA_PATH_ELEMENT +"/variables")
    public Response getVariables(@Auth Optional<User> optionalUser,
                             @Context OgcApi api,
                             @Context ApiRequestContext requestContext,
                             @Context UriInfo uriInfo,
                             @PathParam("collectionId") String collectionId) {
        checkAuthorization(api.getData(), optionalUser);
        checkPathParameter(extensionRegistry, api.getData(), "/collections/{collectionId}/"+ DAPA_PATH_ELEMENT +"/variables", "collectionId", collectionId);

        final boolean includeLinkHeader = api.getData().getExtension(FoundationConfiguration.class)
                .map(FoundationConfiguration::getIncludeLinkHeader)
                .orElse(false);
        final List<Variable> variables = api.getData().getExtension(ObservationProcessingConfiguration.class)
                .map(ObservationProcessingConfiguration::getVariables)
                .orElse(ImmutableList.of());

        ObservationProcessingQueriesHandler.QueryInputVariables queryInput = new ImmutableQueryInputVariables.Builder()
                .collectionId(collectionId)
                .variables(variables)
                .includeLinkHeader(includeLinkHeader)
                .build();

        return queryHandler.handle(ObservationProcessingQueriesHandler.Query.VARIABLES, queryInput, requestContext);
    }
}
