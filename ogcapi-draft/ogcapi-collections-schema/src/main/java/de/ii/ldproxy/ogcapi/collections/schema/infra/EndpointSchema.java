/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.schema.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ldproxy.ogcapi.collections.schema.app.ImmutableQueryInputSchema;
import de.ii.ldproxy.ogcapi.collections.schema.domain.QueriesHandlerSchema;
import de.ii.ldproxy.ogcapi.collections.schema.app.QueriesHandlerSchemaImpl;
import de.ii.ldproxy.ogcapi.collections.schema.domain.SchemaConfiguration;
import de.ii.ldproxy.ogcapi.collections.schema.domain.SchemaFormatExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtendableConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.FoundationConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonConfiguration;
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
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Component
@Provides
@Instantiate
public class EndpointSchema extends EndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointSchema.class);

    private static final List<String> TAGS = ImmutableList.of("Discover data collections");

    private final QueriesHandlerSchema queryHandler;

    public EndpointSchema(@Requires ExtensionRegistry extensionRegistry,
                          @Requires QueriesHandlerSchema queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return SchemaConfiguration.class;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return super.isEnabledForApi(apiData) && apiData.getExtension(GeoJsonConfiguration.class)
            .map(ExtensionConfiguration::isEnabled)
            .orElse(false);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return super.isEnabledForApi(apiData, collectionId) && apiData.getExtension(GeoJsonConfiguration.class, collectionId)
            .map(ExtensionConfiguration::isEnabled)
            .orElse(false);
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(SchemaFormatExtension.class);
        return formats;
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("collections")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_SCHEMA);
        String subSubPath = "/schemas/{type}";
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
                final List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path, collectionId);
                final String operationSummary = "retrieve the schema of features in the feature collection '" + collectionId + "'";
                Optional<String> operationDescription = Optional.empty(); // TODO
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

        return definitionBuilder.build();
    }

    @GET
    @Path("/{collectionId}/schemas/{type}")
    @Produces("application/schema+json")
    public Response getSchema(@Auth Optional<User> optionalUser,
                              @Context OgcApi api,
                              @Context ApiRequestContext requestContext,
                              @Context UriInfo uriInfo,
                              @PathParam("collectionId") String collectionId,
                              @PathParam("type") String type) {

        String definitionPath = "/collections/{collectionId}/schemas/{type}";
        checkPathParameter(extensionRegistry, api.getData(), definitionPath, "collectionId", collectionId);
        checkPathParameter(extensionRegistry, api.getData(), definitionPath, "type", type);

        Optional<String> profile = Optional.ofNullable(requestContext.getParameters().get("profile"));

        QueriesHandlerSchemaImpl.QueryInputSchema queryInput = new ImmutableQueryInputSchema.Builder()
                .from(getGenericQueryInput(api.getData()))
                .collectionId(collectionId)
                .profile(profile)
                .type(type)
                .build();

        return queryHandler.handle(QueriesHandlerSchemaImpl.Query.SCHEMA, queryInput, requestContext);
    }
}
