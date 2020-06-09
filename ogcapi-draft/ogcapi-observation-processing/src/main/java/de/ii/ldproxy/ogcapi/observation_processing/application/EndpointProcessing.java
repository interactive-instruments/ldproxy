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
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.feature_processing.api.ImmutableProcess;
import de.ii.ldproxy.ogcapi.feature_processing.api.ImmutableProcessing;
import de.ii.ldproxy.ogcapi.feature_processing.api.Processing;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.api.*;
import de.ii.xtraplatform.auth.api.User;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class EndpointProcessing extends OgcApiEndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointProcessing.class);
    private static final String DAPA_PATH_ELEMENT = "dapa";
    private static final List<String> TAGS = ImmutableList.of("DAPA");

    @Requires
    I18n i18n;

    // TODO delete
    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("collections")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^/[\\w\\-]+/"+DAPA_PATH_ELEMENT+"/?$")
            .build();

    private final ObservationProcessingQueriesHandler queryHandler;

    public EndpointProcessing(@Requires OgcApiExtensionRegistry extensionRegistry,
                              @Requires ObservationProcessingQueriesHandler queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(ObservationProcessingOutputFormatProcessing.class);
        return formats;
    }

    // TODO delete
    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class);
    }

    @Override
    public OgcApiEndpointDefinition getDefinition(OgcApiApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableOgcApiEndpointDefinition.Builder definitionBuilder = new ImmutableOgcApiEndpointDefinition.Builder()
                    .apiEntrypoint("collections")
                    .sortPriority(10000);
            final String subSubPath = "/"+ DAPA_PATH_ELEMENT;
            final String path = "/collections/{collectionId}" + subSubPath;
            final Set<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
            final Set<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
            final Optional<OgcApiPathParameter> optCollectionIdParam = pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
            if (!optCollectionIdParam.isPresent()) {
                LOGGER.error("Path parameter 'collectionId' missing for resource at path '" + path + "'. The GET method will not be available.");
            } else {
                final  OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
                boolean explode = collectionIdParam.getExplodeInOpenApi();
                final Set<String> collectionIds = (explode) ?
                        collectionIdParam.getValues(apiData) :
                        ImmutableSet.of("{collectionId}");
                collectionIds.stream()
                        .forEach(collectionId -> {
                            String operationSummary = "list the available data retrieval patterns";
                            Optional<String> operationDescription = Optional.empty();
                            String resourcePath = "/collections/" + collectionId + subSubPath;
                            ImmutableOgcApiResourceProcess.Builder resourceBuilder = new ImmutableOgcApiResourceProcess.Builder()
                                    .path(resourcePath)
                                    .pathParameters(pathParameters);
                            OgcApiOperation operation = addOperation(apiData, OgcApiContext.HttpMethods.GET, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, TAGS);
                            if (operation!=null)
                                resourceBuilder.putOperations("GET", operation);
                            definitionBuilder.putResources(resourcePath, resourceBuilder.build());
                        });

            }
            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
    }

    @GET
    @Path("/{collectionId}/"+DAPA_PATH_ELEMENT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProcessing(@Auth Optional<User> optionalUser,
                             @Context OgcApiApi api,
                             @Context OgcApiRequestContext requestContext,
                             @Context UriInfo uriInfo,
                             @PathParam("collectionId") String collectionId) {
        checkAuthorization(api.getData(), optionalUser);
        OgcApiEndpointDefinition definition = extensionRegistry.getExtensionsForType(OgcApiEndpointSubCollection.class)
                .stream()
                .filter(ext -> ext.getClass().getName().equals("de.ii.ldproxy.ogcapi.observation_processing.application.EndpointObservationProcessing"))
                .findAny()
                .map(ext -> ext.getDefinition(api.getData()))
                .orElse(null);
        if (definition==null)
            throw new ServerErrorException("Definition of '/collections/{collectionId}/"+DAPA_PATH_ELEMENT+"' could not be retrieved.", 500);

        URICustomizer uriCustomizer = requestContext.getUriCustomizer()
                .copy()
                .removeLastPathSegments(3)
                .clearParameters();
        Processing processList = ImmutableProcessing.builder()
                .title(i18n.get("processingTitle", requestContext.getLanguage()))
                .description(i18n.get("processingDescription", requestContext.getLanguage()))
                .endpoints(definition.getResources()
                        .entrySet()
                        .stream()
                        .map(entry -> {
                            final String path = entry.getKey();
                            final OgcApiOperation op = entry.getValue().getOperations().get("GET");
                            if (op!=null)
                                return ImmutableProcess.builder()
                                        .title(op.getSummary())
                                        .description(op.getDescription())
                                        .inputCollectionId(collectionId)
                                        .descriptionUri(uriCustomizer.copy()
                                                .ensureLastPathSegment("api")
                                                .addParameter("f","json")
                                                .toString()
                                                + "#/paths/" + path.replace("/","~1"))
                                        .build();
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .build();

        final String path = "/collections/{collectionId}/"+DAPA_PATH_ELEMENT;
        checkPathParameter(extensionRegistry, api.getData(), path, "collectionId", collectionId);

        final FeatureTypeConfigurationOgcApi collectionData = api.getData().getCollections().get(collectionId);
        final OgcApiFeaturesCoreConfiguration coreConfiguration = getExtensionConfiguration(api.getData(), collectionData, OgcApiFeaturesCoreConfiguration.class).orElseThrow(NotFoundException::new);
        final boolean includeHomeLink = getExtensionConfiguration(api.getData(), OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeHomeLink)
                .orElse(false);
        final boolean includeLinkHeader = getExtensionConfiguration(api.getData(), OgcApiCommonConfiguration.class)
                .map(OgcApiCommonConfiguration::getIncludeLinkHeader)
                .orElse(false);

        ObservationProcessingQueriesHandler.OgcApiQueryInputProcessing queryInput = new ImmutableOgcApiQueryInputProcessing.Builder()
                .collectionId(collectionId)
                .processing(processList)
                .includeLinkHeader(includeLinkHeader)
                .includeHomeLink(includeHomeLink)
                .build();

        ObservationProcessingQueriesHandler.Query process = ObservationProcessingQueriesHandlerImpl.Query.LIST;
        return queryHandler.handle(process, queryInput, requestContext);
    }
}
