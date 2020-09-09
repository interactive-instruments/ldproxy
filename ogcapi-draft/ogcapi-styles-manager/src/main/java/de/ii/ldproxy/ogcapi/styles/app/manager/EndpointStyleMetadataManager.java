/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app.manager;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.Endpoint;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.json.domain.JsonConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadata;
import de.ii.ldproxy.ogcapi.styles.domain.StyleMetadataFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.xtraplatform.auth.domain.User;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.ii.ldproxy.ogcapi.domain.FoundationConfiguration.API_RESOURCES_DIR;
import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;

/**
 * update style metadata
 */
@Component
@Provides
@Instantiate
public class EndpointStyleMetadataManager extends Endpoint {

    @Requires
    I18n i18n;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStyleMetadataManager.class);
    private static final List<String> TAGS = ImmutableList.of("Create, update and delete styles");

    private final java.nio.file.Path stylesStore;

    public EndpointStyleMetadataManager(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                                        @Requires ExtensionRegistry extensionRegistry) throws IOException {
        super(extensionRegistry);
        this.stylesStore = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), API_RESOURCES_DIR)
                                    .resolve("styles");
        Files.createDirectories(stylesStore);
    }


    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        Optional<StylesConfiguration> extension = apiData.getExtension(StylesConfiguration.class);

        return extension
                .filter(StylesConfiguration::isEnabled)
                .filter(StylesConfiguration::getManagerEnabled)
                .isPresent();
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

    private boolean isValidationEnabledForApi(OgcApiDataV2 apiData) {
        Optional<StylesConfiguration> extension = apiData.getExtension(StylesConfiguration.class);

        return extension
                .filter(StylesConfiguration::isEnabled)
                .filter(StylesConfiguration::getManagerEnabled)
                .filter(StylesConfiguration::getValidationEnabled)
                .isPresent();
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(StyleMetadataFormatExtension.class)
                    .stream()
                    .filter(StyleMetadataFormatExtension::canSupportTransactions)
                    .collect(Collectors.toList());
        return formats;
    }

    private Map<MediaType, ApiMediaTypeContent> getRequestContent(OgcApiDataV2 apiData, String path, HttpMethods method) {
        return getFormats().stream()
                .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
                .map(f -> f.getRequestContent(apiData, path, method))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(c -> c.getOgcApiMediaType().type(),c -> c));
    }

    @Override
    public ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                    .apiEntrypoint("styles")
                    .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_STYLE_METADATA_MANAGER);
            String path = "/styles/{styleId}/metadata";
            HttpMethods method = HttpMethods.PUT;
            List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
            List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path, method);
            String operationSummary = "update the metadata document of a style";
            Optional<String> operationDescription = Optional.of("Update the style metadata for the style with the id `styleId`. " +
                    "This operation updates the complete metadata document.");
            ImmutableOgcApiResourceData.Builder resourceBuilder = new ImmutableOgcApiResourceData.Builder()
                    .path(path)
                    .pathParameters(pathParameters);
            Map<MediaType, ApiMediaTypeContent> requestContent = getRequestContent(apiData, path, method);
            ApiOperation operation = addOperation(apiData, method, requestContent, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilder.putOperations(method.name(), operation);
            method = HttpMethods.PATCH;
            queryParameters = getQueryParameters(extensionRegistry, apiData, path, method);
            operationSummary = "update parts of the style metadata";
            operationDescription = Optional.of("Update selected elements of the style metadata for " +
                    "the style with the id `styleId`.\n" +
                    "The PATCH semantics in this operation are defined by " +
                    "RFC 7396 (JSON Merge Patch). From the specification:\n" +
                    "\n" +
                    "_'A JSON merge patch document describes changes to be " +
                    "made to a target JSON document using a syntax that " +
                    "closely mimics the document being modified. Recipients " +
                    "of a merge patch document determine the exact set of " +
                    "changes being requested by comparing the content of " +
                    "the provided patch against the current content of the " +
                    "target document. If the provided merge patch contains " +
                    "members that do not appear within the target, those " +
                    "members are added. If the target does contain the " +
                    "member, the value is replaced. Null values in the " +
                    "merge patch are given special meaning to indicate " +
                    "the removal of existing values in the target.'_\n" +
                    "\n" +
                    "Some examples:\n" +
                    "\n" +
                    "To add or update the point of contact, the access" +
                    "constraint and the revision date, just send\n" +
                    "\n" +
                    "```\n" +
                    "{\n" +
                    "  \"pointOfContact\": \"Jane Doe\",\n" +
                    "  \"accessConstraints\": \"restricted\",\n" +
                    "  \"dates\": {\n" +
                    "    \"revision\": \"2019-05-17T11:46:12Z\"\n" +
                    "  }\n" +
                    "}\n" +
                    "```\n" +
                    "\n" +
                    "To remove the point of contact, the access " +
                    "constraint and the revision date, send \n" +
                    "\n" +
                    "```\n" +
                    "{\n" +
                    "  \"pointOfContact\": null,\n" +
                    "  \"accessConstraints\": null,\n" +
                    "  \"dates\": {\n" +
                    "    \"revision\": null\n" +
                    "  }\n" +
                    "}\n" +
                    "```\n" +
                    "\n" +
                    "For arrays the complete array needs to be sent. " +
                    "To add a keyword to the example style metadata object, send\n" +
                    "\n" +
                    "```\n" +
                    "{\n" +
                    "  \"keywords\": [ \"basemap\", \"TDS\", \"TDS 6.1\", \"OGC API\", \"new keyword\" ]\n" +
                    "}\n" +
                    "```\n" +
                    "\n" +
                    "To remove the \"TDS\" keyword, send\n" +
                    "\n" +
                    "```\n" +
                    "{\n" +
                    "  \"keywords\": [ \"basemap\", \"TDS 6.1\", \"OGC API\", \"new keyword\" ]\n" +
                    "}\n" +
                    "```\n" +
                    "\n" +
                    "To remove the keywords, send\n" +
                    "\n" +
                    "```\n" +
                    "{\n" +
                    "  \"keywords\": null\n" +
                    "}\n" +
                    "```\n" +
                    "\n" +
                    "The same applies to `stylesheets` and `layers`. To update " +
                    "these members, you have to send the complete new array value.");
            requestContent = getRequestContent(apiData, path, method);
            operation = addOperation(apiData, method, requestContent, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilder.putOperations(method.name(), operation);
            definitionBuilder.putResources(path, resourceBuilder.build());

            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
    }

    /**
     * updates the metadata document of a style
     *
     * @param styleId the local identifier of a specific style
     * @return empty response (204)
     */
    @Path("/{styleId}/metadata")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putStyleMetadata(@Auth Optional<User> optionalUser, @PathParam("styleId") String styleId,
                                     @Context OgcApi dataset, @Context ApiRequestContext ogcApiRequest,
                                     @Context HttpServletRequest request, byte[] requestBody) {

        checkAuthorization(dataset.getData(), optionalUser);

        boolean newStyle = isNewStyle(dataset.getId(), styleId);
        if (newStyle) {
            throw new NotFoundException(MessageFormat.format("The style ''{0}'' does not exist in this API.", styleId));
        }

        // prepare Jackson mapper for deserialization
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            // parse input for validation
            mapper.readValue(requestBody, StyleMetadata.class);
            putStyleDocument(dataset.getId(), styleId, "metadata", requestBody);
        } catch (IOException e) {
            throw new BadRequestException(e);
        }

        return Response.noContent()
                       .build();
    }

    /**
     * partial update to the metadata of a style
     *
     * @param styleId the local identifier of a specific style
     * @return empty response (204)
     */
    @Path("/{styleId}/metadata")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    public Response patchStyleMetadata(@Auth Optional<User> optionalUser, @PathParam("styleId") String styleId,
                                       @Context OgcApi api, @Context ApiRequestContext ogcApiRequest,
                                       @Context HttpServletRequest request, byte[] requestBody) {

        checkAuthorization(api.getData(), optionalUser);

        boolean newStyle = isNewStyle(api.getId(), styleId);
        if (newStyle) {
            throw new NotFoundException(MessageFormat.format("The style ''{0}'' does not exist in this API.", styleId));
        }

        // prepare Jackson mapper for deserialization
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Map<String, Object> currentMetadata;
        Map<String, Object> updatedMetadata;
        try {
            // parse input for validation
            mapper.readValue(requestBody, StyleMetadata.class);
            File metadataFile = new File( stylesStore + File.separator + api.getId() + File.separator + styleId + ".metadata");
            currentMetadata = mapper.readValue(metadataFile, new TypeReference<LinkedHashMap>() {
            });
            ObjectReader objectReader = mapper.readerForUpdating(currentMetadata);
            updatedMetadata = objectReader.readValue(requestBody);
            Optional<JsonConfiguration> jsonConfig = api.getData()
                                                        .getExtension(JsonConfiguration.class);
            byte[] updatedMetadataString = jsonConfig.isPresent() && jsonConfig.get().getUseFormattedJsonOutput() ?
                    mapper.writerWithDefaultPrettyPrinter()
                          .writeValueAsBytes(updatedMetadata) :
                    mapper.writeValueAsBytes(updatedMetadata);
            putStyleDocument(api.getId(), styleId, "metadata", updatedMetadataString);
        } catch (IOException e) {
            throw new BadRequestException(e);
        }

        return Response.noContent()
                       .build();
    }

    private boolean isNewStyle(String datasetId, String styleId) {

        File metadataFile = new File( stylesStore + File.separator + datasetId + File.separator + styleId + ".metadata");
        return !metadataFile.exists();
    }

    /**
     * search for the style in the store and update it, or create a new one
     *  @param datasetId       the dataset id
     * @param styleId           the id of the style, for which a document should be updated or created
     * @param fileExtension     the type of document
     * @param payload the new Style as a byte array
     */
    private void putStyleDocument(String datasetId, String styleId, String fileExtension, byte[] payload) {

        String key = styleId + "." + fileExtension;
        File styleFile = new File(stylesStore + File.separator + datasetId + File.separator + styleId + "." + fileExtension);

        try {
            Files.write(styleFile.toPath(), payload);
        } catch (IOException e) {
            throw new ServerErrorException("could not PUT style document: "+styleId, 500);
        }
    }

    // TODO: move elsewhere
    /**
     * create MediaType from text string; if the input string has problems, the value defaults to wildcards
     *
     * @param mediaTypeString the media type as a string
     * @return the processed media type
     */
    public static MediaType mediaTypeFromString(String mediaTypeString) {
        String[] typeAndSubtype = mediaTypeString.split("/", 2);
        if (typeAndSubtype[0].matches("application|audio|font|example|image|message|model|multipart|text|video")) {
            if (typeAndSubtype.length==1) {
                // no subtype
                return new MediaType(typeAndSubtype[0],"*");
            } else {
                // we have a subtype - and maybe parameters
                String[] subtypeAndParameters = typeAndSubtype[1].split(";");
                int count = subtypeAndParameters.length;
                if (count==1) {
                    // no parameters
                    return new MediaType(typeAndSubtype[0],subtypeAndParameters[0]);
                } else {
                    // we have at least one parameter
                    Map<String, String> params = IntStream.rangeClosed(1, count-1)
                            .mapToObj( i -> subtypeAndParameters[i].split("=",2) )
                            .filter(nameValuePair -> nameValuePair.length==2)
                            .map(nameValuePair -> new AbstractMap.SimpleImmutableEntry<>(nameValuePair[0].trim(), nameValuePair[1].trim()))
                            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
                    return new MediaType(typeAndSubtype[0],subtypeAndParameters[0],params);
                }
            }
        } else {
            // not a valid type, fall back to wildcard
            return MediaType.WILDCARD_TYPE;
        }
    }
}
