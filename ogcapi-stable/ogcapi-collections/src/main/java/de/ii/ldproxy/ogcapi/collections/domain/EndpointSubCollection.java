/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ApiHeader;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ApiRequestBody;
import de.ii.ldproxy.ogcapi.domain.Endpoint;
import de.ii.ldproxy.ogcapi.domain.Example;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.ExternalDocumentation;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiOperation;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiRequestBody;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiResponse;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.ParameterExtension;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class EndpointSubCollection extends Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointSubCollection.class);

    /**
     *
     * @param extensionRegistry
     */
    public EndpointSubCollection(ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return super.isEnabledForApi(apiData) &&
                apiData.getCollections()
                        .values()
                        .stream()
                        .filter(FeatureTypeConfigurationOgcApi::getEnabled)
                        .anyMatch(featureType -> isEnabledForApi(apiData, featureType.getId()));
}

    protected ImmutableApiOperation addOperation(OgcApiDataV2 apiData, HttpMethods method,
                                                 List<OgcApiQueryParameter> queryParameters, String collectionId, String subSubPath,
                                                 String operationSummary, Optional<String> operationDescription, List<String> tags) {
        return addOperation(apiData, method, queryParameters, ImmutableList.of(), collectionId, subSubPath, operationSummary, operationDescription, Optional.empty(), ImmutableMap.of(), tags, false, false);
    }

    protected ImmutableApiOperation addOperation(OgcApiDataV2 apiData, HttpMethods method, boolean postUrlencoded,
                                                 List<OgcApiQueryParameter> queryParameters, String collectionId, String subSubPath,
                                                 String operationSummary, Optional<String> operationDescription, List<String> tags) {
        return addOperation(apiData, method, queryParameters, ImmutableList.of(), collectionId, subSubPath, operationSummary, operationDescription, Optional.empty(), ImmutableMap.of(), tags, false, postUrlencoded);
    }

    protected ImmutableApiOperation addOperation(OgcApiDataV2 apiData, HttpMethods method,
                                                 List<OgcApiQueryParameter> queryParameters, List<ApiHeader> headers,
                                                 String collectionId, String subSubPath,
                                                 String operationSummary, Optional<String> operationDescription, List<String> tags) {
        return addOperation(apiData, method, queryParameters, headers, collectionId, subSubPath, operationSummary, operationDescription, Optional.empty(), ImmutableMap.of(), tags, false, false);
    }

    protected ImmutableApiOperation addOperation(OgcApiDataV2 apiData, HttpMethods method,
                                                 List<OgcApiQueryParameter> queryParameters, String collectionId, String subSubPath,
                                                 String operationSummary, Optional<String> operationDescription, List<String> tags,
                                                 boolean hide) {
        return addOperation(apiData, method, queryParameters, ImmutableList.of(), collectionId, subSubPath, operationSummary, operationDescription, Optional.empty(), ImmutableMap.of(), tags, hide, false);
    }

    protected ImmutableApiOperation addOperation(OgcApiDataV2 apiData, HttpMethods method,
                                                 List<OgcApiQueryParameter> queryParameters, String collectionId, String subSubPath,
                                                 String operationSummary, Optional<String> operationDescription,
                                                 Optional<ExternalDocumentation> externalDocs, Map<String, List<Example>> examples, List<String> tags) {
        return addOperation(apiData, method, queryParameters, ImmutableList.of(), collectionId, subSubPath, operationSummary, operationDescription, externalDocs, examples, tags, false, false);
    }

    protected ImmutableApiOperation addOperation(OgcApiDataV2 apiData, HttpMethods method,
                                                 List<OgcApiQueryParameter> queryParameters, String collectionId, String subSubPath,
                                                 String operationSummary, Optional<String> operationDescription,
                                                 Optional<ExternalDocumentation> externalDocs, Map<String, List<Example>> examples, List<String> tags,
                                                 boolean hide) {
        return addOperation(apiData, method, queryParameters, ImmutableList.of(), collectionId, subSubPath, operationSummary, operationDescription, externalDocs, examples, tags, hide, false);
    }

    protected ImmutableApiOperation addOperation(OgcApiDataV2 apiData, HttpMethods method,
                                                 List<OgcApiQueryParameter> queryParameters, List<ApiHeader> headers,
                                                 String collectionId, String subSubPath,
                                                 String operationSummary, Optional<String> operationDescription,
                                                 Optional<ExternalDocumentation> externalDocs, Map<String, List<Example>> examples, List<String> tags,
                                                 boolean hide, boolean postUrlencoded) {
        final String path = "/collections/"+collectionId+subSubPath;
        ApiRequestBody body = null;
        if (method== HttpMethods.POST && postUrlencoded) {
            Schema formSchema = new ObjectSchema();
            queryParameters
                .forEach(param -> {
                    Schema paramSchema = param.getSchema(apiData, collectionId)
                        .description(param.getDescription());
                    formSchema.addProperties(param.getName(), paramSchema);
                    if (param.getRequired(apiData, collectionId))
                        formSchema.addRequiredItem(param.getName());
                });
            Map<MediaType, ApiMediaTypeContent> requestContent =
                    ImmutableMap.of(MediaType.APPLICATION_FORM_URLENCODED_TYPE,
                                    new ImmutableApiMediaTypeContent.Builder().ogcApiMediaType(new ImmutableApiMediaType.Builder()
                                                                                                                    .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                                                                                                                    .label("Form")
                                                                                                                    .parameter("form")
                                                                                                                    .build())
                                                                              .schema(formSchema)
                                                                              .schemaRef("#/components/schemas/form_"+collectionId)
                                                                              .build());
            body = new ImmutableApiRequestBody.Builder()
                    .description("The query parameters of the GET request encoded in the request body.")
                    .content(requestContent)
                    .build();
        } else if (method== HttpMethods.POST || method== HttpMethods.PUT || method== HttpMethods.PATCH) {
            Map<MediaType, ApiMediaTypeContent> requestContent = collectionId.startsWith("{") ?
                    getRequestContent(apiData, Optional.empty(), subSubPath, method) :
                    getRequestContent(apiData, Optional.of(collectionId), subSubPath, method);
            if (requestContent.isEmpty()) {
                LOGGER.error("No media type supported for the resource at path '" + path + "'. The " + method + " method will not be available.");
                return null;
            }
            body = new ImmutableApiRequestBody.Builder()
                    .description(method== HttpMethods.POST ? "The new resource to be added." : "The new resource to be added or updated.")
                    .content(requestContent)
                    .build();
        }
        Map<MediaType, ApiMediaTypeContent> responseContent = collectionId.startsWith("{") ?
                getContent(apiData, Optional.empty(), subSubPath, postUrlencoded ? HttpMethods.GET : method) :
                getContent(apiData, Optional.of(collectionId), subSubPath, postUrlencoded ? HttpMethods.GET : method);
        if (method== HttpMethods.GET && responseContent.isEmpty()) {
            LOGGER.error("No media type supported for the resource at path '" + path + "'. The GET method will not be available.");
            return null;
        } else if (method== HttpMethods.POST && postUrlencoded && responseContent.isEmpty()) {
            LOGGER.error("No media type supported for the resource at path '" + path + "'. The POST method will not be available.");
            return null;
        }
        if (!examples.isEmpty()) {
            responseContent.entrySet().stream()
                    .forEach(entry -> {
                        List<Example> exs = examples.get(entry.getKey().toString());
                        if (!exs.isEmpty()) {
                            entry.setValue(new ImmutableApiMediaTypeContent.Builder()
                                    .from(entry.getValue())
                                    .examples(exs)
                                    .build());
                        }
                    });
        }
        ImmutableApiResponse.Builder responseBuilder = new ImmutableApiResponse.Builder()
                .statusCode(postUrlencoded ? Endpoint.SUCCESS_STATUS_RESOURCE.get(HttpMethods.GET) : Endpoint.SUCCESS_STATUS_RESOURCE.get(method))
                .description("The operation was executed successfully.")
                .headers(headers.stream().filter(header -> header.isResponseHeader()).collect(Collectors.toUnmodifiableList()));
        if (!responseContent.isEmpty())
            responseBuilder.content(responseContent);
        ImmutableApiOperation.Builder operationBuilder = new ImmutableApiOperation.Builder()
                .summary(operationSummary)
                .description(operationDescription)
                .externalDocs(externalDocs)
                .tags(tags)
                .queryParameters(postUrlencoded ? ImmutableList.of() : queryParameters)
                .headers(headers.stream().filter(header -> header.isRequestHeader()).collect(Collectors.toUnmodifiableList()))
                .success(responseBuilder.build())
                .hideInOpenAPI(hide);
        if (body!=null)
            operationBuilder.requestBody(body);
        return operationBuilder.build();
    }

    /**
     *
     * @param apiData
     * @param collectionId
     * @param subSubPath
     * @return
     */
    protected Map<MediaType, ApiMediaTypeContent> getContent(OgcApiDataV2 apiData, Optional<String> collectionId, String subSubPath, HttpMethods method) {
        return getFormats().stream()
                .filter(outputFormatExtension -> collectionId.isPresent() ? outputFormatExtension.isEnabledForApi(apiData, collectionId.get()) : outputFormatExtension.isEnabledForApi(apiData))
                .map(f -> f.getContent(apiData, "/collections/"+collectionId.orElse("{collectionId}")+subSubPath, method))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(c -> c.getOgcApiMediaType().type(),c -> c));
    }

    /**
     *
     * @param apiData
     * @param collectionId
     * @param subSubPath
     * @return
     */
    protected Map<MediaType, ApiMediaTypeContent> getRequestContent(OgcApiDataV2 apiData, Optional<String> collectionId, String subSubPath, HttpMethods method) {
        return getFormats().stream()
                .filter(outputFormatExtension -> collectionId.isPresent() ? outputFormatExtension.isEnabledForApi(apiData, collectionId.get()) : outputFormatExtension.isEnabledForApi(apiData))
                .map(f -> f.getRequestContent(apiData, "/collections/"+collectionId.orElse("{collectionId}")+subSubPath, method))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(c -> c.getOgcApiMediaType().type(),c -> c));
    }

    /**
     *
     * @param apiData
     * @param collectionId
     */
    protected void checkCollectionExists(@Context OgcApiDataV2 apiData,
                               @PathParam("collectionId") String collectionId) {
        if (!apiData.isCollectionEnabled(collectionId)) {
            throw new NotFoundException(MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));
        }
    }

    public ImmutableList<OgcApiQueryParameter> getQueryParameters(ExtensionRegistry extensionRegistry, OgcApiDataV2 apiData, String definitionPath, String collectionId) {
        return getQueryParameters(extensionRegistry, apiData, definitionPath, collectionId, HttpMethods.GET);
    }

    public ImmutableList<OgcApiQueryParameter> getQueryParameters(ExtensionRegistry extensionRegistry, OgcApiDataV2 apiData, String definitionPath, String collectionId, HttpMethods method) {
        if (collectionId.equals("{collectionId}")) {
            Optional<String> representativeCollectionId = getRepresentativeCollectionId(apiData);
            if (representativeCollectionId.isEmpty())
                return getQueryParameters(extensionRegistry, apiData, definitionPath, method);

            collectionId = representativeCollectionId.get();
        }
        String finalCollectionId = collectionId;
        return extensionRegistry.getExtensionsForType(OgcApiQueryParameter.class)
                                .stream()
                                .filter(param -> param.isApplicable(apiData, definitionPath, finalCollectionId, method))
                                .sorted(Comparator.comparing(ParameterExtension::getName))
                                .collect(ImmutableList.toImmutableList());
    }

    public ImmutableList<ApiHeader> getHeaders(ExtensionRegistry extensionRegistry, OgcApiDataV2 apiData, String definitionPath, String collectionId, HttpMethods method) {
        // TODO or do we need collection-specific headers?
        return getHeaders(extensionRegistry, apiData, definitionPath, method);
    }

    protected Optional<String> getRepresentativeCollectionId(OgcApiDataV2 apiData) {
        if (apiData.getExtension(CollectionsConfiguration.class)
                   .filter(config -> config.getCollectionDefinitionsAreIdentical()
                                           .orElse(false))
                   .isPresent())
            return Optional.ofNullable(apiData.getCollections()
                                              .keySet()
                                              .iterator()
                                              .next());

        return Optional.empty();
    }

    /* TODO do we need collection-specific path parameters? The API definitions would need to be adapted for this, too
    ImmutableList<OgcApiPathParameter> getPathParameters(ExtensionRegistry extensionRegistry, OgcApiDataV2 apiData, String definitionPath, String collectionId) {
        return extensionRegistry.getExtensionsForType(OgcApiPathParameter.class)
                .stream()
                .filter(param -> param.isApplicable(apiData, definitionPath, collectionId))
                .collect(ImmutableSet.toImmutableSet());
    }
    */
}
