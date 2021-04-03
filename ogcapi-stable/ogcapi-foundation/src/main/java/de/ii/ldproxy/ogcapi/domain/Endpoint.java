/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.StringSchema;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Endpoint implements EndpointExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(Endpoint.class);

    protected static final Map<HttpMethods, String> SUCCESS_STATUS = ImmutableMap.of(
            HttpMethods.GET, "200",
            HttpMethods.POST, "201",
            HttpMethods.PUT, "204",
            HttpMethods.PATCH, "204",
            HttpMethods.DELETE, "204"
    );

    protected final ExtensionRegistry extensionRegistry;
    private final Map<Integer, ApiEndpointDefinition> apiDefinitions;
    protected List<? extends FormatExtension> formats;

    /**
     *
     * @param extensionRegistry
     */
    public Endpoint(ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
        this.apiDefinitions =  new HashMap<>();
        this.formats = null;
    }

    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, MODE apiValidation) {
        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
                .mode(apiValidation);

        try {
            if (getFormats().isEmpty()) {
                builder.addStrictErrors(MessageFormat.format("The Endpoint class ''{0}'' does not support any output format.", this.getClass().getSimpleName()));
            }

            // compile and cache the API definition
            getDefinition(apiData);

        } catch (Exception exception) {
            String message = exception.getMessage();
            if (Objects.isNull(message))
                message = exception.getClass().getSimpleName() + " at " + exception.getStackTrace()[0].toString();
            builder.addErrors(message);
        }

        return builder.build();
    }

    @Override
    public final ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
        if (!isEnabledForApi(apiData)) {
            return EndpointExtension.super.getDefinition(apiData);
        }

        return apiDefinitions.computeIfAbsent(apiData.hashCode(), ignore -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Generating API definition for {}", this.getClass().getSimpleName());
            }

            ApiEndpointDefinition apiEndpointDefinition = computeDefinition(apiData);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Finished generating API definition for {}", this.getClass().getSimpleName());
            }

            return apiEndpointDefinition;
        });
    }

    protected abstract ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData);

    /**
     *
     * @return the list of output format candidates for this endpoint
     */
    public abstract List<? extends FormatExtension> getFormats();

    protected ApiOperation addOperation(OgcApiDataV2 apiData, List<OgcApiQueryParameter> queryParameters, String path,
                                        String operationSummary, Optional<String> operationDescription, List<String> tags) {
        return addOperation(apiData, queryParameters, path, operationSummary, operationDescription, Optional.empty(), tags);
    }

    protected ApiOperation addOperation(OgcApiDataV2 apiData, List<OgcApiQueryParameter> queryParameters, String path,
                                        String operationSummary, Optional<String> operationDescription,
                                        Optional<ExternalDocumentation> externalDocs, List<String> tags) {
        Map<MediaType, ApiMediaTypeContent> responseContent = getContent(apiData, path);
        if (responseContent.isEmpty()) {
            LOGGER.error("No media type supported for resource at path '" + path + "'. The GET method will not be available.");
            return null;
        }

        ApiResponse response = new ImmutableApiResponse.Builder()
                .description("The operation was executed successfully.")
                .content(responseContent)
                .build();
        return new ImmutableApiOperation.Builder()
                .summary(operationSummary)
                .description(operationDescription)
                .externalDocs(externalDocs)
                .tags(tags)
                .queryParameters(queryParameters)
                .success(response)
                .build();
    }

    protected ApiOperation addOperation(OgcApiDataV2 apiData, HttpMethods method, Map<MediaType, ApiMediaTypeContent> content,
                                        List<OgcApiQueryParameter> queryParameters, String path,
                                        String operationSummary, Optional<String> operationDescription, List<String> tags) {
        return addOperation(apiData, method, content, queryParameters, path, operationSummary, operationDescription, Optional.empty(), tags);
    }

    protected ApiOperation addOperation(OgcApiDataV2 apiData, HttpMethods method, Map<MediaType, ApiMediaTypeContent> content,
                                        List<OgcApiQueryParameter> queryParameters, String path,
                                        String operationSummary, Optional<String> operationDescription,
                                        Optional<ExternalDocumentation> externalDocs, List<String> tags) {
        ImmutableApiResponse.Builder responseBuilder = new ImmutableApiResponse.Builder()
                .statusCode(SUCCESS_STATUS.get(method))
                .description("The operation was executed successfully.");
        if (method== HttpMethods.GET && !content.isEmpty())
            responseBuilder.content(content);
        if (method== HttpMethods.POST)
            responseBuilder.putHeaders("Location",new Header().schema(new StringSchema()).description("The URI of the new resource."));
        ImmutableApiOperation.Builder operationBuilder = new ImmutableApiOperation.Builder()
                .summary(operationSummary)
                .description(operationDescription)
                .externalDocs(externalDocs)
                .tags(tags)
                .queryParameters(queryParameters)
                .success(responseBuilder.build());
        if ((method== HttpMethods.POST || method== HttpMethods.PUT || method== HttpMethods.PATCH) && content!=null)
            operationBuilder.requestBody(new ImmutableApiRequestBody.Builder()
                    .content(content)
                    .description(method== HttpMethods.POST ? "The new resource to be added." : "The new resource to be added or updated.")
                    .build());
        return operationBuilder.build();
    }

    /**
     *
     * @param queryParameters
     * @return
     */
    protected Map<String, String> toFlatMap(MultivaluedMap<String, String> queryParameters) {
        return toFlatMap(queryParameters, false);
    }

    /**
     *
     * @param queryParameters
     * @param keysToLowerCase
     * @return
     */
    protected Map<String, String> toFlatMap(MultivaluedMap<String, String> queryParameters,
                                  boolean keysToLowerCase) {
        return queryParameters.entrySet()
                .stream()
                .map(entry -> {
                    String key = keysToLowerCase ? entry.getKey()
                            .toLowerCase() : entry.getKey();
                    return new AbstractMap.SimpleImmutableEntry<>(key, entry.getValue()
                            .isEmpty() ? "" : entry.getValue()
                            .get(0));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected Map<MediaType, ApiMediaTypeContent> getContent(OgcApiDataV2 apiData, String path) {
        return getFormats().stream()
                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
                .map(f -> f.getContent(apiData, path))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(c -> c.getOgcApiMediaType().type(),c -> c));
    }

    protected QueryInput getGenericQueryInput(OgcApiDataV2 apiData) {
        final boolean includeLinkHeader = apiData.getExtension(FoundationConfiguration.class)
                .map(FoundationConfiguration::getIncludeLinkHeader)
                .orElse(false);

        QueryInput queryInput = new ImmutableQueryInputGeneric.Builder()
                .includeLinkHeader(includeLinkHeader)
                .build();

        return queryInput;
    }
}
