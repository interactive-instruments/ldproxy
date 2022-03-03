/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.google.common.collect.ImmutableSet;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.immutables.value.Value;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Value.Immutable
public abstract class ApiEndpointDefinition {

    // stable 0 - 999
    public static final int SORT_PRIORITY_LANDING_PAGE = 0;
    public static final int SORT_PRIORITY_CONFORMANCE = 1;
    public static final int SORT_PRIORITY_API_DEFINITION = 2;
    public static final int SORT_PRIORITY_COLLECTIONS = 10;
    public static final int SORT_PRIORITY_COLLECTION = 11;
    public static final int SORT_PRIORITY_FEATURES = 100;

    // draft 1000 - 9999
    public static final int SORT_PRIORITY_FEATURES_TRANSACTION = 1000;
    public static final int SORT_PRIORITY_FEATURES_JSONLD_CONTEXT = 1100;
    public static final int SORT_PRIORITY_QUERYABLES = 1200;
    public static final int SORT_PRIORITY_SORTABLES = 1250;
    public static final int SORT_PRIORITY_SCHEMA = 1300;
    public static final int SORT_PRIORITY_FEATURES_EXTENSIONS = 1400;
    public static final int SORT_PRIORITY_TILE_SETS = 1500;
    public static final int SORT_PRIORITY_TILE_SET = 1510;
    public static final int SORT_PRIORITY_TILE = 1520;
    public static final int SORT_PRIORITY_TILE_SETS_COLLECTION = 1530;
    public static final int SORT_PRIORITY_TILE_SET_COLLECTION = 1540;
    public static final int SORT_PRIORITY_TILE_COLLECTION = 1550;
    public static final int SORT_PRIORITY_MAP_TILE_SETS = 1600;
    public static final int SORT_PRIORITY_MAP_TILE_SET = 1610;
    public static final int SORT_PRIORITY_MAP_TILE = 1620;
    public static final int SORT_PRIORITY_MAP_TILE_SETS_COLLECTION = 1630;
    public static final int SORT_PRIORITY_MAP_TILE_SET_COLLECTION = 1640;
    public static final int SORT_PRIORITY_MAP_TILE_COLLECTION = 1650;
    public static final int SORT_PRIORITY_TILE_MATRIX_SETS = 1700;
    public static final int SORT_PRIORITY_STYLES = 2000;
    public static final int SORT_PRIORITY_STYLESHEET = 2010;
    public static final int SORT_PRIORITY_STYLE_METADATA = 2020;
    public static final int SORT_PRIORITY_STYLES_MANAGER = 2030;
    public static final int SORT_PRIORITY_STYLE_METADATA_MANAGER = 2040;
    public static final int SORT_PRIORITY_STYLES_COLLECTION = 2050;
    public static final int SORT_PRIORITY_STYLESHEET_COLLECTION = 2060;
    public static final int SORT_PRIORITY_STYLE_METADATA_COLLECTION = 2070;
    public static final int SORT_PRIORITY_STYLES_MANAGER_COLLECTION = 2080;
    public static final int SORT_PRIORITY_STYLE_METADATA_MANAGER_COLLECTION = 2090;
    public static final int SORT_PRIORITY_RESOURCES = 2100;
    public static final int SORT_PRIORITY_RESOURCE = 2110;
    public static final int SORT_PRIORITY_RESOURCES_MANAGER = 2120;
    public static final int SORT_PRIORITY_ROUTES_POST = 2500;
    public static final int SORT_PRIORITY_ROUTES_GET = 2510;
    public static final int SORT_PRIORITY_ROUTE_GET = 2520;
    public static final int SORT_PRIORITY_ROUTE_DELETE = 2530;
    public static final int SORT_PRIORITY_ROUTE_DEFINITION = 2540;

    public static final int SORT_PRIORITY_DUMMY = Integer.MAX_VALUE;

  /**
     *
     * @return the entrypoint resource for this definition, all sub-paths are relative to this base path
     */
    public abstract String getApiEntrypoint();

    /**
     *
     * @param subPath
     * @return
     */
    public String getPath(String subPath) {
        return  "/" + getApiEntrypoint() + subPath;
    }

    /**
     *
     * @return
     */
    public abstract int getSortPriority();

    /**
     *
     * @return a map of API paths to the resource at the path
     */
    public abstract Map<String, OgcApiResource> getResources();

    /**
     * Checks, if a request is supported by this endpoint based on the API path and the HTTP method
     * @param requestPath the path of the resource
     * @param method the HTTP method that; set to {@code null} for checking against all methods
     * @return flag, whether the endpoint supports the request
     */
    public boolean matches(String requestPath, String method) {
        return getOperation(requestPath, method).isPresent();
    }

    /**
     * Checks, if a request is supported by this endpoint based on the API path and the HTTP method
     * @param resource the resource
     * @param method the HTTP method that; set to {@code null} for checking against all methods
     * @return the operation that supports the request
     */
    public Optional<ApiOperation> getOperation(OgcApiResource resource, String method) {
        // at least one method is supported?
        if (method==null)
            return resource.getOperations().values().stream().findAny();

        // support HEAD for all GETs
        if (method.equals("HEAD"))
            return Optional.ofNullable(resource.getOperations().get("GET"));

        return Optional.ofNullable(resource.getOperations().get(method));
    }

    /**
     * Checks, if a request is supported by this endpoint based on the API path and the HTTP method
     * @param requestPath the path of the resource
     * @param method the HTTP method that; set to {@code null} for checking against all methods
     * @return the operation that supports the request
     */
    public Optional<ApiOperation> getOperation(String requestPath, String method) {
        Optional<OgcApiResource> resource = getResource(requestPath);

        if (!resource.isPresent())
            return Optional.empty();

        return getOperation(resource.get(), method);
    }

    /**
     * Checks, if a request may be supported by this endpoint based on the API path
     * @param requestPath the path of the resource
     * @return the resource that supports the request
     */
    public Optional<OgcApiResource> getResource(String requestPath) {
        OgcApiResource resource = getResources().get(requestPath);
        if (resource==null)
            // if nothing was found, replace path parameters with their pattern
            resource = getResources().values().stream()
                    .filter(r -> r.getPathPatternCompiled().matcher(requestPath).matches())
                    .findAny()
                    .orElse(null);

        return Optional.ofNullable(resource);
    }

    /**
     *
     * @param openAPI the OpenAPI definition without the endpoint
     * @return the updated OpenAPI definition
     */
    public OpenAPI updateOpenApiDefinition(OgcApiDataV2 apiData, OpenAPI openAPI) {
        getResources().values()
                .stream()
                .sorted(Comparator.comparing(OgcApiResource::getPath))
                .forEachOrdered(resource -> {
                    String path = resource.getPath();
                    // skip the API definition
                    if (path.startsWith("/api"))
                        return;
                    Optional<String> collectionId = resource.getCollectionId(apiData);
                    PathItem pathItem = openAPI.getPaths().get(path);
                    if (pathItem==null)
                        pathItem = new PathItem();
                    for (Map.Entry<String, ApiOperation> operationEntry : resource.getOperations().entrySet()) {
                        String method = operationEntry.getKey();
                        boolean status400 = false;
                        boolean status404 = false;
                        boolean status405 = true;
                        boolean status406 = method.equals("GET");
                        boolean status415 = method.equals("POST") || method.equals("PUT") || method.equals("PATCH");
                        boolean status422 = method.equals("POST");
                        ApiOperation operation = operationEntry.getValue();
                        if (!operation.getSuccess().isPresent())
                            // skip
                            continue;
                        if (operation.getHideInOpenAPI())
                            // skip
                            continue;
                        Operation op = new Operation();
                        op.summary(operation.getSummary());
                        op.description(operation.getDescription().orElse(null));
                        if (operation.getExternalDocs().isPresent()) {
                            ExternalDocumentation externalDocs = operation.getExternalDocs().get();
                            io.swagger.v3.oas.models.ExternalDocumentation docs = new io.swagger.v3.oas.models.ExternalDocumentation().url(externalDocs.getUrl());
                            if (externalDocs.getDescription().isPresent())
                                docs.description(externalDocs.getDescription().get());
                            op.externalDocs(docs);
                        }
                        operation.getTags().stream().forEach(tag -> op.addTagsItem(tag));
                        if (operation.getOperationId().isPresent())
                            op.operationId(operation.getOperationId().get());
                        for (OgcApiPathParameter param : resource.getPathParameters()) {
                            if (param.getExplodeInOpenApi(apiData))
                                continue;
                            Parameter p = openAPI.getComponents().getParameters().get(param.getId(collectionId));
                            if (p == null) {
                                p = new io.swagger.v3.oas.models.parameters.PathParameter();
                                p.name(param.getName());
                                p.description(param.getDescription());
                                p.required(param.getRequired(apiData, collectionId));
                                p.schema(param.getSchema(apiData, collectionId));
                                openAPI.getComponents().addParameters(param.getId(collectionId), p);
                            }
                            op.addParametersItem(new Parameter().$ref("#/components/parameters/" + param.getId(collectionId)));
                            status404 = true;
                        }
                        for (OgcApiQueryParameter param : operation.getQueryParameters()) {
                            Parameter p = openAPI.getComponents().getParameters().get(param.getId(collectionId));
                            if (p == null) {
                                p = new io.swagger.v3.oas.models.parameters.QueryParameter();
                                p.name(param.getName());
                                p.description(param.getDescription());
                                p.required(param.getRequired(apiData, collectionId));
                                p.schema(param.getSchema(apiData, collectionId));
                                p.style(Parameter.StyleEnum.valueOf(param.getStyle().toUpperCase()));
                                p.explode(param.getExplode());
                                openAPI.getComponents().addParameters(param.getId(collectionId), p);
                            }
                            op.addParametersItem(new Parameter().$ref("#/components/parameters/" + param.getId(collectionId)));
                            status400 = true;
                        }
                        boolean isMutation = false;
                        switch (method) {
                            case "GET":
                                pathItem.get(op);
                                break;
                            case "POST":
                                pathItem.post(op);
                                isMutation = true;
                                if (operation.getRequestBody().isPresent()) {
                                    Set<javax.ws.rs.core.MediaType> mediaTypes = operation.getRequestBody().get().getContent().keySet();
                                    if (mediaTypes.size()==1 && mediaTypes.iterator().next().equals(javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE)) {
                                        // URL-encoded form
                                        isMutation = false;
                                        status406 = true;
                                    } else if (operation.getSuccess().stream().anyMatch(response -> ImmutableSet.of("200", "202", "204").contains(response.getStatusCode()))) {
                                        // Processing request
                                        isMutation = false;
                                        status406 = true;
                                    }
                                }
                                break;
                            case "PUT":
                                pathItem.put(op);
                                isMutation = true;
                                break;
                            case "DELETE":
                                pathItem.delete(op);
                                isMutation = true;
                                break;
                            case "PATCH":
                                pathItem.patch(op);
                                isMutation = true;
                                break;
                            default:
                                // skip HEAD and OPTIONS, these are not included in the OpenAPI definition
                                continue;
                        }
                        Optional<ApiRequestBody> requestBody = operation.getRequestBody();
                        if (requestBody.isPresent()) {
                            RequestBody body = new RequestBody();
                            body.description(requestBody.get().getDescription());
                            body.required(requestBody.get().getRequired());
                            Content content = new Content();
                            requestBody.get().getContent()
                                    .entrySet()
                                    .stream()
                                    .forEach(entry -> {
                                        MediaType contentForMediaType = new MediaType();
                                        String schemaRef = entry.getValue().getSchemaRef();
                                        contentForMediaType.schema(new Schema().$ref(schemaRef));
                                        if (schemaRef.startsWith("#/components/schemas/")) {
                                            String schemaId = schemaRef.substring(21);
                                            if (!openAPI.getComponents().getSchemas().containsKey(schemaId))
                                                openAPI.getComponents().getSchemas().put(schemaId, entry.getValue().getSchema());
                                        }
                                        addExamples(entry.getValue().getExamples(), contentForMediaType);
                                        content.addMediaType(entry.getKey().toString(),contentForMediaType);
                                    });
                            body.content(content);
                            op.requestBody(body);
                            status400 = true;
                        }

                        for (ApiHeader header : operation.getHeaders()) {
                            op.addParametersItem(new Parameter().in("header")
                                                                .name(header.getId())
                                                                .description(header.getDescription())
                                                                .schema(header.getSchema(apiData)));
                        }

                        ApiResponse success = operation.getSuccess().get();
                        ApiResponses responses = new ApiResponses();

                        io.swagger.v3.oas.models.responses.ApiResponse response = new io.swagger.v3.oas.models.responses.ApiResponse();
                        response.description(success.getDescription());
                        for (ApiHeader header : success.getHeaders()) {
                            response.addHeaderObject(header.getId(), new Header().description(header.getDescription())
                                                                                 .schema(header.getSchema(apiData)));
                        }
                        Content content = new Content();
                        success.getContent()
                                .entrySet()
                                .stream()
                                .forEach(entry -> {
                                    MediaType contentForMediaType = new MediaType();
                                    String schemaRef = entry.getValue().getSchemaRef();
                                    contentForMediaType.schema(new Schema().$ref(schemaRef));
                                    if (schemaRef.startsWith("#/components/schemas/")) {
                                        String schemaId = schemaRef.substring(21);
                                        if (!openAPI.getComponents().getSchemas().containsKey(schemaId))
                                            openAPI.getComponents().getSchemas().put(schemaId, entry.getValue().getSchema());
                                    }
                                    addExamples(entry.getValue().getExamples(), contentForMediaType);
                                    content.addMediaType(entry.getKey().toString(),contentForMediaType);
                                });
                        response.content(content);
                        responses.addApiResponse(success.getStatusCode(), response);

                        if (status400) {
                            response = new io.swagger.v3.oas.models.responses.ApiResponse();
                            response.description("Bad Request");
                            responses.addApiResponse("400", response);
                        }

                        if (status404) {
                            response = new io.swagger.v3.oas.models.responses.ApiResponse();
                            response.description("Not Found");
                            responses.addApiResponse("404", response);
                        }

                        if (status405) {
                            response = new io.swagger.v3.oas.models.responses.ApiResponse();
                            response.description("Method Not Allowed");
                            responses.addApiResponse("405", response);
                        }

                        if (status406) {
                            response = new io.swagger.v3.oas.models.responses.ApiResponse();
                            response.description("Not Acceptable");
                            responses.addApiResponse("406", response);
                        }

                        if (status415) {
                            response = new io.swagger.v3.oas.models.responses.ApiResponse();
                            response.description("Unsupported Media Type");
                            responses.addApiResponse("415", response);
                        }

                        if (status422) {
                            response = new io.swagger.v3.oas.models.responses.ApiResponse();
                            response.description("Unprocessable Entity");
                            responses.addApiResponse("422", response);
                        }

                        response = new io.swagger.v3.oas.models.responses.ApiResponse();
                        response.description("Server Error");
                        responses.addApiResponse("500", response);

                        op.responses(responses);

                        if (apiData.getSecured() && isMutation) {
                            op.addSecurityItem(new SecurityRequirement().addList("JWT"));
                        }
                    }
                    openAPI.path(path, pathItem);
                });

        return openAPI;
    }

    private void addExamples(List<Example> examples, MediaType mediaType) {
        examples.stream()
                .filter(example -> example.getValue().isPresent())
                .findFirst()
                .ifPresent(example -> mediaType.setExample(example.getValue().get()));

        /* Just set "example" as Swagger UI does not show "examples"
        int i = 1;
        boolean exSet = false;
        for (Example example : examples) {
            Example ex = new Example();
            if (example.getSummary().isPresent())
                ex.setSummary(example.getSummary().get());
            if (example.getDescription().isPresent())
                ex.setDescription(example.getDescription().get());
            if (example.getValue().isPresent()) {
                ex.setValue(example.getValue().get());
                // we also have to set the separate example as only this is visible in the Swagger HTML
                if (!exSet) {
                    mediaType.setExample(example.getValue().get());
                    exSet = true;
                }
            }
            if (example.getExternalValue().isPresent())
                ex.setExternalValue(example.getExternalValue().get());
            mediaType.addExamples(String.valueOf(i++), ex);
        }
         */
    }
}
