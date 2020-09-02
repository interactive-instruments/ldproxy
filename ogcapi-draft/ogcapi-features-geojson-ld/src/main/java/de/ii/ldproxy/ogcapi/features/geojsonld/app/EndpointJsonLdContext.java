/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojsonld.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.geojsonld.domain.GeoJsonLdConfiguration;
import io.swagger.v3.oas.models.media.ObjectSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static de.ii.ldproxy.ogcapi.domain.FoundationConfiguration.API_RESOURCES_DIR;
import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;


@Component
@Provides
@Instantiate
public class EndpointJsonLdContext extends EndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointJsonLdContext.class);
    private static final List<String> TAGS = ImmutableList.of("Discover data collections");

    private static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application","ld+json"))
            .label("JSON-LD")
            .parameter("json")
            .build();

    private final java.nio.file.Path contextDirectory;

    EndpointJsonLdContext(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                          @Requires ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
        this.contextDirectory = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), API_RESOURCES_DIR)
                                     .resolve("json-ld-contexts");
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return GeoJsonLdConfiguration.class;
    }

    private java.nio.file.Path getContextPath(String apiId, String collectionId) {
        return contextDirectory.resolve(apiId)
                               .resolve(collectionId + ".jsonld");
    }

    @Path("/{collectionId}/context")
    @GET
    @Produces("application/ld+json")
    public Response getContext(@Context ApiRequestContext apiRequestContext, @Context OgcApi api,
                               @PathParam("collectionId") String collectionId) throws IOException {

        java.nio.file.Path context = getContextPath(api.getId(), collectionId);

        if (!Files.isRegularFile(context)) {
            throw new NotFoundException("The JSON-LD context was not found.");
        }

        // TODO validate, that it is a valid JSON-LD Context document

        return Response.ok(Files.newInputStream(context),"application/ld+json")
                       .build();
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        return ImmutableList.of();
    }

    @Override
    public ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                    .apiEntrypoint("collections")
                    .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_FEATURES_JSONLD_CONTEXT);
            String subSubPath = "/context";
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
                    if (explode && !Files.isRegularFile(getContextPath(apiData.getId(), collectionId)))
                        // skip, if no context is available
                        continue;
                    final List<OgcApiQueryParameter> queryParameters = explode ?
                            getQueryParameters(extensionRegistry, apiData, path, collectionId) :
                            getQueryParameters(extensionRegistry, apiData, path);
                    final String operationSummary = "retrieve the JSON-LD context for the feature collection '" + collectionId + "'";
                    Optional<String> operationDescription = Optional.empty();
                    String resourcePath = "/collections/" + collectionId + subSubPath;
                    ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                            .path(resourcePath)
                            .pathParameters(pathParameters);
                    Map<MediaType, ApiMediaTypeContent> responseContent = new ImmutableMap.Builder<MediaType, ApiMediaTypeContent>()
                            .put(MEDIA_TYPE.type(),
                                 new ImmutableApiMediaTypeContent.Builder()
                                    .ogcApiMediaType(MEDIA_TYPE)
                                    .schema(new ObjectSchema())
                                    .schemaRef("#/components/schemas/json-ld-context")
                                    .build())
                            .build();
                    ApiOperation operation = addOperation(apiData, HttpMethods.GET, responseContent, queryParameters, resourcePath, operationSummary, operationDescription, TAGS);
                    if (operation!=null)
                        resourceBuilder.putOperations("GET", operation);
                    definitionBuilder.putResources(resourcePath, resourceBuilder.build());
                }
            }
            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
    }
}
