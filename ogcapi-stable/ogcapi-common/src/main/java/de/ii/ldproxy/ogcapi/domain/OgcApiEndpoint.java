package de.ii.ldproxy.ogcapi.domain;

import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.*;
import java.util.stream.Collectors;

public abstract class OgcApiEndpoint implements OgcApiEndpointExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiEndpoint.class);

    protected static final Map<OgcApiContext.HttpMethods, String> SUCCESS_STATUS = ImmutableMap.of(
            OgcApiContext.HttpMethods.GET, "200",
            OgcApiContext.HttpMethods.POST, "201",
            OgcApiContext.HttpMethods.PUT, "204",
            OgcApiContext.HttpMethods.PATCH, "204",
            OgcApiContext.HttpMethods.DELETE, "204"
    );

    protected final OgcApiExtensionRegistry extensionRegistry;
    protected Map<String,OgcApiEndpointDefinition> apiDefinitions;
    protected List<? extends FormatExtension> formats;

    /**
     *
     * @param extensionRegistry
     */
    public OgcApiEndpoint(OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
        this.apiDefinitions =  new HashMap<>();
        this.formats = null;
    }

    /**
     *
     * @return the list of output format candidates for this endpoint
     */
    public abstract List<? extends FormatExtension> getFormats();

    protected OgcApiOperation addOperation(OgcApiApiDataV2 apiData, Set<OgcApiQueryParameter> queryParameters, String path,
                                           String operationSummary, Optional<String> operationDescription, List<String> tags) {
        Map<MediaType, OgcApiMediaTypeContent> responseContent = getContent(apiData, path);
        if (responseContent.isEmpty()) {
            LOGGER.error("No media type supported for resource at path '" + path + "'. The GET method will not be available.");
            return null;
        }

        OgcApiResponse response = new ImmutableOgcApiResponse.Builder()
                .description("The operation was executed successfully.")
                .content(responseContent)
                .build();
        return new ImmutableOgcApiOperation.Builder()
                .summary(operationSummary)
                .description(operationDescription)
                .tags(tags)
                .queryParameters(queryParameters)
                .success(response)
                .build();
    }

    protected OgcApiOperation addOperation(OgcApiApiDataV2 apiData, OgcApiContext.HttpMethods method, Map<MediaType, OgcApiMediaTypeContent> content,
                                           Set<OgcApiQueryParameter> queryParameters, String path,
                                           String operationSummary, Optional<String> operationDescription, List<String> tags) {
        ImmutableOgcApiResponse.Builder responseBuilder = new ImmutableOgcApiResponse.Builder()
                .statusCode(SUCCESS_STATUS.get(method))
                .description("The operation was executed successfully.");
        if (method== OgcApiContext.HttpMethods.GET && !content.isEmpty())
            responseBuilder.content(content);
        if (method==OgcApiContext.HttpMethods.POST)
            responseBuilder.putHeaders("Location",new Header().schema(new StringSchema()).description("The URI of the new resource."));
        ImmutableOgcApiOperation.Builder operationBuilder = new ImmutableOgcApiOperation.Builder()
                .summary(operationSummary)
                .description(operationDescription)
                .tags(tags)
                .queryParameters(queryParameters)
                .success(responseBuilder.build());
        if ((method==OgcApiContext.HttpMethods.POST || method==OgcApiContext.HttpMethods.PUT || method==OgcApiContext.HttpMethods.PATCH) && content!=null)
            operationBuilder.requestBody(new ImmutableOgcApiRequestBody.Builder()
                    .content(content)
                    .description(method==OgcApiContext.HttpMethods.POST ? "The new resource to be added." : "The new resource to be added or updated.")
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

    protected Map<MediaType, OgcApiMediaTypeContent> getContent(OgcApiApiDataV2 apiData, String path) {
        return getFormats().stream()
                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
                .map(f -> f.getContent(apiData, path))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(c -> c.getOgcApiMediaType().type(),c -> c));
    }
}
