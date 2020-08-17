/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.collections.domain.OgcApiEndpointSubCollection;
import de.ii.ldproxy.ogcapi.domain.*;
import io.swagger.v3.oas.models.media.ObjectSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
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

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

@Component
@Provides
@Instantiate
public class JsonLdContextEndpoint extends OgcApiEndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonLdContextEndpoint.class);
    private static final List<String> TAGS = ImmutableList.of("Access data");

    private static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application","ld+json"))
            .label("JSON-LD")
            .parameter("json")
            .build();

    private final java.nio.file.Path contextDirectory;

    JsonLdContextEndpoint(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                          @Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
        this.contextDirectory = Paths.get(bundleContext.getProperty(DATA_DIR_KEY))
                                     .resolve("json-ld-contexts");
    }

    @Override
    protected Class getConfigurationClass() {
        return GeoJsonConfiguration.class;
    }

    @Path("/{collectionId}/context")
    @GET
    @Produces("application/ld+json")
    public Response getContext(@Context OgcApiRequestContext wfs3Request, @Context OgcApiApi service,
                               @PathParam("collectionId") String collectionId) throws IOException {

        java.nio.file.Path context = contextDirectory.resolve(collectionId);

        if (!Files.isRegularFile(context)) {
            throw new NotFoundException("The JSON-LD context was not found.");
        }

        return Response.ok(Files.newInputStream(context),"application/ld+json")
                       .build();
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        return ImmutableList.of();
    }

    @Override
    public OgcApiEndpointDefinition getDefinition(OgcApiApiDataV2 apiData) {
        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableOgcApiEndpointDefinition.Builder definitionBuilder = new ImmutableOgcApiEndpointDefinition.Builder()
                    .apiEntrypoint("collections")
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_FEATURES_JSONLD_CONTEXT);
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
                    if (explode && !Files.isRegularFile(contextDirectory.resolve(collectionId)))
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
                    Map<MediaType, OgcApiMediaTypeContent> responseContent = new ImmutableMap.Builder<MediaType, OgcApiMediaTypeContent>()
                            .put(MEDIA_TYPE.type(),
                                 new ImmutableOgcApiMediaTypeContent.Builder()
                                    .ogcApiMediaType(MEDIA_TYPE)
                                    .schema(new ObjectSchema())
                                    .schemaRef("#/components/schemas/json-ld-context")
                                    .build())
                            .build();
                    OgcApiOperation operation = addOperation(apiData, OgcApiContext.HttpMethods.GET, responseContent, queryParameters, resourcePath, operationSummary, operationDescription, TAGS);
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
