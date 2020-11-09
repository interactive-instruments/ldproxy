/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.infra;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.collections.app.ImmutableQueryInputFeatureCollection;
import de.ii.ldproxy.ogcapi.collections.app.QueriesHandlerCollections;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsConfiguration;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsFormatExtension;
import de.ii.ldproxy.ogcapi.collections.domain.EndpointSubCollection;
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
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.auth.domain.User;
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
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Provides
@Instantiate
public class EndpointCollection extends EndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointCollection.class);
    private static final List<String> TAGS = ImmutableList.of("Discover data collections");

    @Requires
    private QueriesHandlerCollections queryHandler;

    public EndpointCollection(@Requires ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return CollectionsConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(CollectionsFormatExtension.class);
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
                    .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_COLLECTION);
            String path = "/collections/{collectionId}";
            List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
            List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
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
                    ApiOperation operation = addOperation(apiData, HttpMethods.GET, queryParameters, collectionId, "", operationSummary, operationDescription, TAGS);
                    if (operation!=null)
                        resourceBuilder.putOperations("GET", operation);
                    definitionBuilder.putResources(resourcePath, resourceBuilder.build());
                }
            }

            apiDefinitions.put(apiDataHash, definitionBuilder.build());
        }

        return apiDefinitions.get(apiDataHash);
    }

    @GET
    @Path("/{collectionId}")
    public Response getCollection(@Auth Optional<User> optionalUser, @Context OgcApi api,
                                  @Context ApiRequestContext requestContext, @PathParam("collectionId") String collectionId) {
        checkAuthorization(api.getData(), optionalUser);

        if (!api.getData().isCollectionEnabled(collectionId)) {
            throw new NotFoundException(MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));
        }

        boolean includeLinkHeader = api.getData()
                                       .getExtension(FoundationConfiguration.class)
                                       .map(FoundationConfiguration::getIncludeLinkHeader)
                                       .orElse(false);
        List<Link> additionalLinks = api.getData()
                                        .getCollections()
                                        .get(collectionId)
                                        .getAdditionalLinks();

        QueriesHandlerCollections.QueryInputFeatureCollection queryInput = new ImmutableQueryInputFeatureCollection.Builder()
                .collectionId(collectionId)
                .includeLinkHeader(includeLinkHeader)
                .additionalLinks(additionalLinks)
                .build();

        return queryHandler.handle(QueriesHandlerCollections.Query.FEATURE_COLLECTION, queryInput, requestContext);
    }
}
