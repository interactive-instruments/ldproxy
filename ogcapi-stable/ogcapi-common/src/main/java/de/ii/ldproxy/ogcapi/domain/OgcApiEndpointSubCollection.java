package de.ii.ldproxy.ogcapi.domain;

import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.StringSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public abstract class OgcApiEndpointSubCollection extends OgcApiEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiEndpointSubCollection.class);

    /**
     *
     * @param extensionRegistry
     */
    public OgcApiEndpointSubCollection(OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    protected ImmutableOgcApiOperation addOperation(OgcApiApiDataV2 apiData, OgcApiContext.HttpMethods method,
                                                    List<OgcApiQueryParameter> queryParameters, String collectionId, String subSubPath,
                                                    String operationSummary, Optional<String> operationDescription, List<String> tags) {
        return addOperation(apiData, method, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, Optional.empty(), tags, false);
    }

    protected ImmutableOgcApiOperation addOperation(OgcApiApiDataV2 apiData, OgcApiContext.HttpMethods method,
                                                    List<OgcApiQueryParameter> queryParameters, String collectionId, String subSubPath,
                                                    String operationSummary, Optional<String> operationDescription, List<String> tags,
                                                    boolean hide) {
        return addOperation(apiData, method, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, Optional.empty(), tags, hide);
    }

    protected ImmutableOgcApiOperation addOperation(OgcApiApiDataV2 apiData, OgcApiContext.HttpMethods method,
                                                    List<OgcApiQueryParameter> queryParameters, String collectionId, String subSubPath,
                                                    String operationSummary, Optional<String> operationDescription,
                                                    Optional<OgcApiExternalDocumentation> externalDocs, List<String> tags) {
        return addOperation(apiData, method, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, externalDocs, tags, false);
    }

    protected ImmutableOgcApiOperation addOperation(OgcApiApiDataV2 apiData, OgcApiContext.HttpMethods method,
                                                    List<OgcApiQueryParameter> queryParameters, String collectionId, String subSubPath,
                                                    String operationSummary, Optional<String> operationDescription,
                                                    Optional<OgcApiExternalDocumentation> externalDocs, List<String> tags,
                                                    boolean hide) {
        final String path = "/collections/"+collectionId+subSubPath;
        OgcApiRequestBody body = null;
        if (method==OgcApiContext.HttpMethods.POST || method==OgcApiContext.HttpMethods.PUT || method==OgcApiContext.HttpMethods.PATCH) {
            Map<MediaType, OgcApiMediaTypeContent> requestContent = collectionId.startsWith("{") ?
                    getRequestContent(apiData, Optional.empty(), subSubPath, method) :
                    getRequestContent(apiData, Optional.of(collectionId), subSubPath, method);
            if (requestContent.isEmpty()) {
                LOGGER.error("No media type supported for the resource at path '" + path + "'. The " + method.toString() + " method will not be available.");
                return null;
            }
            body = new ImmutableOgcApiRequestBody.Builder()
                    .description(method==OgcApiContext.HttpMethods.POST ? "The new resource to be added." : "The new resource to be added or updated.")
                    .content(requestContent)
                    .build();
        }
        Map<MediaType, OgcApiMediaTypeContent> responseContent = collectionId.startsWith("{") ?
                getContent(apiData, Optional.empty(), subSubPath, method) :
                getContent(apiData, Optional.of(collectionId), subSubPath, method);
        if (method==OgcApiContext.HttpMethods.GET && responseContent.isEmpty()) {
            LOGGER.error("No media type supported for the resource at path '" + path + "'. The GET method will not be available.");
            return null;
        }
        ImmutableOgcApiResponse.Builder responseBuilder = new ImmutableOgcApiResponse.Builder()
                .statusCode(SUCCESS_STATUS.get(method))
                .description("The operation was executed successfully.");
        if (!responseContent.isEmpty())
            responseBuilder.content(responseContent);
        if (method==OgcApiContext.HttpMethods.POST)
            responseBuilder.putHeaders("Location",new Header().schema(new StringSchema()).description("The URI of the new resource."));
        ImmutableOgcApiOperation.Builder operationBuilder = new ImmutableOgcApiOperation.Builder()
                .summary(operationSummary)
                .description(operationDescription)
                .externalDocs(externalDocs)
                .tags(tags)
                .queryParameters(queryParameters)
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
    protected Map<MediaType, OgcApiMediaTypeContent> getContent(OgcApiApiDataV2 apiData, Optional<String> collectionId, String subSubPath, OgcApiContext.HttpMethods method) {
        return getFormats().stream()
                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
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
    protected Map<MediaType, OgcApiMediaTypeContent> getRequestContent(OgcApiApiDataV2 apiData, Optional<String> collectionId, String subSubPath, OgcApiContext.HttpMethods method) {
        return getFormats().stream()
                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
                .map(f -> f.getRequestContent(apiData, "/collections/"+collectionId.orElse("{collectionId}")+subSubPath, method))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(c -> c.getOgcApiMediaType().type(),c -> c));
    }

    /**
     *
     * @param apiData
     * @param collectionId
     */
    protected void checkCollectionExists(@Context OgcApiApiDataV2 apiData,
                               @PathParam("collectionId") String collectionId) {
        if (!apiData.isCollectionEnabled(collectionId)) {
            throw new NotFoundException(MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));
        }
    }

    public ImmutableList<OgcApiQueryParameter> getQueryParameters(OgcApiExtensionRegistry extensionRegistry, OgcApiApiDataV2 apiData, String definitionPath, String collectionId) {
        return getQueryParameters(extensionRegistry, apiData, definitionPath, collectionId, OgcApiContext.HttpMethods.GET);
    }

    public ImmutableList<OgcApiQueryParameter> getQueryParameters(OgcApiExtensionRegistry extensionRegistry, OgcApiApiDataV2 apiData, String definitionPath, String collectionId, OgcApiContext.HttpMethods method) {
        return extensionRegistry.getExtensionsForType(OgcApiQueryParameter.class)
                .stream()
                .filter(param -> param.isApplicable(apiData, definitionPath, collectionId, method))
                .sorted(Comparator.comparing(OgcApiParameter::getName))
                .collect(ImmutableList.toImmutableList());
    }

    /* TODO do we need collection-specific path parameters? The API definitions would need to be adapted for this, too
    ImmutableList<OgcApiPathParameter> getPathParameters(OgcApiExtensionRegistry extensionRegistry, OgcApiApiDataV2 apiData, String definitionPath, String collectionId) {
        return extensionRegistry.getExtensionsForType(OgcApiPathParameter.class)
                .stream()
                .filter(param -> param.isApplicable(apiData, definitionPath, collectionId))
                .collect(ImmutableSet.toImmutableSet());
    }
    */
}
