/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app.manager;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
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
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableStyleMetadata;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableStyleSheet;
import de.ii.ldproxy.ogcapi.styles.domain.StylesLinkGenerator;
import de.ii.ldproxy.ogcapi.styles.domain.MbStyleStylesheet;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StyleSheet;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.xtraplatform.auth.domain.User;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.ii.ldproxy.ogcapi.domain.FoundationConfiguration.API_RESOURCES_DIR;
import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;

/**
 * creates, updates and deletes a style from the service
 */
@Component
@Provides
@Instantiate
public class EndpointStylesManager extends Endpoint implements ConformanceClass {

    @Requires
    I18n i18n;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStylesManager.class);
    private static final List<String> TAGS = ImmutableList.of("Create, update and delete styles");

    private final java.nio.file.Path stylesStore;

    public EndpointStylesManager(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                                 @Requires ExtensionRegistry extensionRegistry) throws IOException {
        super(extensionRegistry);
        this.stylesStore = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), API_RESOURCES_DIR)
                                .resolve("styles");
        Files.createDirectories(stylesStore);
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-styles-1/0.0/conf/manage-styles");
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
            formats = extensionRegistry.getExtensionsForType(StyleFormatExtension.class)
                    .stream()
                    .filter(StyleFormatExtension::canSupportTransactions)
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

        int apiDataHash = apiData.hashCode();
        if (!apiDefinitions.containsKey(apiDataHash)) {
            Optional<StylesConfiguration> stylesExtension = apiData.getExtension(StylesConfiguration.class);
            ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                    .apiEntrypoint("styles")
                    .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_STYLES_MANAGER);
            String path = "/styles";
            List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path, HttpMethods.POST);
            String operationSummary = "add a new style";
            String description = "Adds a style to the style repository";
            if (stylesExtension.isPresent() && stylesExtension.get().getValidationEnabled()) {
                description += " or just validates a style.\n" +
                        "If the parameter `validate` is set to `yes`, the style will be validated before adding " +
                        "the style to the server. If the parameter `validate` is set to `only`, the server will " +
                        "not be changed and only the validation result will be returned";
            }
            description += ".\n" +
                    "If a new style is created, the following rules apply:\n" +
                    "* If the style submitted in the request body includes an identifier (this depends on " +
                    "the style encoding), that identifier will be used. If a style with that identifier " +
                    "already exists, an error is returned.\n" +
                    "* If no identifier can be determined from the submitted style, the server will assign " +
                    "a new identifier to the style.\n" +
                    "* A minimal style metadata resource is created at `/styles/{styleId}/metadata`. Please " +
                    "update the metadata using a PUT request to keep the style metadata consistent with " +
                    "the style definition.\n" +
                    "* The URI of the new style is returned in the header `Location`.\n";
            Optional<String> operationDescription = Optional.of(description);
            ImmutableOgcApiResourceData.Builder resourceBuilder = new ImmutableOgcApiResourceData.Builder()
                    .path(path);
            Map<MediaType, ApiMediaTypeContent> requestContent = getRequestContent(apiData, path, HttpMethods.POST);
            ApiOperation operation = addOperation(apiData, HttpMethods.POST, requestContent, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilder.putOperations("POST", operation);
            definitionBuilder.putResources(path, resourceBuilder.build());
            path = "/styles/{styleId}";
            ImmutableList<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
            queryParameters = getQueryParameters(extensionRegistry, apiData, path, HttpMethods.PUT);
            operationSummary = "replace a style or add a new style";
            description = "Replace an existing style with the id `styleId`. If no such style exists, " +
                    "a new style with that id is added.\n";
            if (stylesExtension.isPresent() && stylesExtension.get().getValidationEnabled()) {
                description +=
                        "If the parameter `validate` is set to `yes`, the style will be validated before adding " +
                                "the style to the server. If the parameter `validate` is set to `only`, the server will " +
                                "not be changed and only the validation result will be returned.\n";
            }
            description += "For updated styles, the style metadata resource at `/styles/{styleId}/metadata` " +
                    "is not updated. For new styles a minimal style metadata resource is created. Please " +
                    "update the metadata using a PUT request to keep the style metadata consistent with " +
                    "the style definition.";
            operationDescription = Optional.of(description);
            resourceBuilder = new ImmutableOgcApiResourceData.Builder()
                    .path(path)
                    .pathParameters(pathParameters);
            requestContent = getRequestContent(apiData, path, HttpMethods.PUT);
            operation = addOperation(apiData, HttpMethods.PUT, requestContent, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilder.putOperations("PUT", operation);
            queryParameters = getQueryParameters(extensionRegistry, apiData, path, HttpMethods.DELETE);
            operationSummary = "delete a style";
            operationDescription = Optional.of("Delete an existing style with the id `styleId`. If no such style exists, " +
                    "an error is returned. Deleting a style also deletes the subordinate resources, " +
                    "i.e., the style metadata.");
            requestContent = getRequestContent(apiData, path, HttpMethods.DELETE);
            operation = addOperation(apiData, HttpMethods.DELETE, requestContent, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilder.putOperations("DELETE", operation);
            definitionBuilder.putResources(path, resourceBuilder.build());

            apiDefinitions.put(apiDataHash, definitionBuilder.build());
        }

        return apiDefinitions.get(apiDataHash);
    }

    /**
     * creates a new style
     *
     * @return empty response (201), with Location header
     */
    @Path("/")
    @POST
    //TODO: replace with */*, return error when no match is found
    @Consumes({"application/vnd.mapbox.style+json", "application/vnd.ogc.sld+xml;version=1.0", "application/vnd.ogc.sld+xml;version=1.1"})
    public Response postStyle(@Auth Optional<User> optionalUser,
                              @QueryParam("validate") String validate,
                              @Context OgcApi api,
                              @Context ApiRequestContext ogcApiRequest,
                              @Context HttpServletRequest request,
                              byte[] requestBody) {

        checkAuthorization(api.getData(), optionalUser);
        String datasetId = api.getId();

        String contentType = request.getContentType();
        MediaType requestMediaType = EndpointStylesManager.mediaTypeFromString(contentType);

        boolean val = Objects.nonNull(validate) && validate.matches("yes|only");
        String styleId = "*";

        for (StyleFormatExtension format: extensionRegistry.getExtensionsForType(StyleFormatExtension.class)) {
            MediaType formatMediaType = format.getMediaType().type();
            if (format.isEnabledForApi(api.getData()) && requestMediaType.isCompatible(formatMediaType)) {

                //TODO: move validation to StyleFormatExtension
                if (Objects.equals(format.getMediaType()
                                         .type()
                                         .toString(), "application/vnd.mapbox.style+json")) {
                    JsonNode requestBodyJson = validateRequestBodyMbStyle(requestBody, val);
                    styleId = requestBodyJson.get("name")
                            .asText();
                } else if (Objects.equals(format.getMediaType()
                                                .type()
                                                .toString(), "application/vnd.ogc.sld+xml;version=1.0")) {
                    URL xsd = Resources.getResource(EndpointStylesManager.class, "/sld10.xsd");
                    validateRequestBodyMbStyle(requestBody, val, xsd);
                }

                if (val && validate.equals("only"))
                    return Response.noContent()
                            .build();

                Pattern styleNamePattern = Pattern.compile("[^\\w]", Pattern.CASE_INSENSITIVE);
                Matcher styleNameMatcher = styleNamePattern.matcher(styleId);
                if (!isNewStyle(datasetId, styleId)) {
                    throw new WebApplicationException(Response.Status.CONFLICT); // TODO
                } else if (styleId.contains(" ") || styleNameMatcher.find()) {
                    int id = 0;
                    while (!isNewStyle(datasetId, Integer.toString(id))) {
                        id++;
                    }
                    styleId = Integer.toString(id);
                }

                writeStylesheet(api.getData(), ogcApiRequest, styleId, format, requestBody, true);
                break;
            }
        }

        // Return 201 with Location header
        URI newURI;
        try {
            newURI = ogcApiRequest.getUriCustomizer()
                                  .copy()
                                  .ensureLastPathSegment(styleId)
                                  .build();
        } catch (URISyntaxException e) {
            throw new ServerErrorException(500); // TODO
        }

        return Response.created(newURI)
                       .build();
    }

    /**
     * creates or updates a style(sheet)
     *
     * @param styleId the local identifier of a specific style
     * @return empty response (204)
     */
    @Path("/{styleId}")
    @PUT
    //TODO: replace with */*, return error when no match is found
    @Consumes({"application/vnd.mapbox.style+json", "application/vnd.ogc.sld+xml;version=1.0", "application/vnd.ogc.sld+xml;version=1.1"})
    public Response putStyle(@Auth Optional<User> optionalUser,
                             @PathParam("styleId") String styleId,
                             @QueryParam("validate") String validate,
                             @Context OgcApi dataset,
                             @Context ApiRequestContext ogcApiRequest,
                             @Context HttpServletRequest request,
                             byte[] requestBody) {

        checkAuthorization(dataset.getData(), optionalUser);

        boolean newStyle = isNewStyle(dataset.getId(), styleId);
        String contentType = request.getContentType();
        MediaType requestMediaType = EndpointStylesManager.mediaTypeFromString(contentType);

        boolean val = Objects.nonNull(validate) && validate.matches("yes|only");

        for (StyleFormatExtension format: extensionRegistry.getExtensionsForType(StyleFormatExtension.class)) {
            MediaType formatMediaType = format.getMediaType().type();
            if (format.isEnabledForApi(dataset.getData()) && requestMediaType.isCompatible(formatMediaType)) {

                //TODO: move validation to StyleFormatExtension
                if (Objects.equals(format.getMediaType()
                                         .type()
                                         .toString(), "application/vnd.ogc.sld+xml;version=1.0")) {
                    JsonNode requestBodyJson = validateRequestBodyMbStyle(requestBody, val);
                } else if (Objects.equals(format.getMediaType()
                                                .type()
                                                .toString(), "application/vnd.ogc.sld+xml;version=1.0")) {
                    URL xsd = Resources.getResource(EndpointStylesManager.class, "/sld10.xsd");
                    validateRequestBodyMbStyle(requestBody, val, xsd);
                }

                if (val && validate.equals("only"))
                    return Response.noContent()
                            .build();

                writeStylesheet(dataset.getData(), ogcApiRequest, styleId, format, requestBody, newStyle);

                // TODO: add stylesheet to metadata, if newStyle == false

                break;
            }
        }

        return Response.noContent()
                       .build();
    }

    private boolean isNewStyle(String datasetId, String styleId) {

        File metadataFile = new File( stylesStore + File.separator + datasetId + File.separator + styleId + ".metadata");
        return !metadataFile.exists();
    }

    private void writeStylesheet(OgcApiDataV2 datasetData, ApiRequestContext ogcApiRequest, String styleId,
                                 StyleFormatExtension format, byte[] requestBody, boolean newStyle) {

        String datasetId = datasetData.getId();

        try {
            putStyleDocument(datasetId, styleId, format.getFileExtension(), requestBody);

            if (newStyle) {
                final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

                ImmutableStyleSheet.Builder stylesheet = ImmutableStyleSheet.builder()
                                                                            .native_(true)
                                                                            .link(stylesLinkGenerator.generateStylesheetLink(ogcApiRequest.getUriCustomizer(),
                                                                                    styleId, format.getMediaType(),
                                                                                    i18n, ogcApiRequest.getLanguage()))
                                                                            .specification(format.getSpecification())
                                                                            .version(format.getVersion());

                List<StyleSheet> stylesheets = new ArrayList<>();
                stylesheets.add(stylesheet.build());

                ImmutableStyleMetadata.Builder metadata = ImmutableStyleMetadata.builder()
                                                                                .id(styleId)
                                                                                .title(styleId)
                                                                                .stylesheets(stylesheets);

                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new Jdk8Module());
                byte[] metadataRequestBody = mapper.writeValueAsBytes(metadata.build());
                putStyleDocument(datasetId, styleId, "metadata", metadataRequestBody);
            }
        } catch (Exception e) {
            // something went wrong, clean up
            deleteStyle(datasetId, styleId);
            //
            throw new ServerErrorException(500); // TODO: details
        }
    }

    /**
     * deletes a style
     *
     * @param styleId the local identifier of a specific style
     * @return empty response (204)
     */
    @Path("/{styleId}")
    @DELETE
    public Response deleteStyle(@Auth Optional<User> optionalUser, @PathParam("styleId") String styleId,
                                @Context OgcApi dataset) {

        checkAuthorization(dataset.getData(), optionalUser);

        deleteStyle(dataset.getId(), styleId);

        return Response.noContent()
                       .build();
    }

    /**
     * search for the style in the store and delete it (i.e., all style documents)
     *  @param datasetId the key value store
     * @param styleId     the id of the style, that should be deleted
     */
    private void deleteStyle(String datasetId, String styleId) {

        boolean styleFound = false;
        File apiDir = new File(stylesStore + File.separator + datasetId);

        for (String key : apiDir.list()) {
            if (key.substring(0, key.lastIndexOf("."))
                   .equals(styleId)) {
                styleFound = true;
                File styleFile = new File( apiDir + File.separator + key );
                if (styleFile.exists())
                    styleFile.delete();
            }
        }
        if (!styleFound) {
            throw new NotFoundException(MessageFormat.format("The style ''{0}'' does not exist in this API.", styleId));
        }
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

    /**
     * checks if the request body from the PUT-Request has valid content
     *
     * @param requestBody the new Style as a JsonNode
     * @return true if content is valid
     */
    public static void validateRequestBody(JsonNode requestBody) {

        // TODO: review
        JsonNode version = requestBody.get("version");
        JsonNode sources = requestBody.get("sources");
        JsonNode layers = requestBody.get("layers");

        if (layers == null) {
            throw new BadRequestException("The Mapbox Style document has no layers.");
        }
        if (version == null) {
            throw new BadRequestException("The Mapbox Style document has no version.");
        }
        if (version.isInt() && version.intValue() != 8) {
            throw new BadRequestException("The Mapbox Style document does not have version '8'. Found: " + version.asText());
        }
        if (sources == null) {
            throw new BadRequestException("The Mapbox Style document has no sources.");
        }
        int size = layers.size();
        List<String> ids = new ArrayList<>();
        List<String> types = ImmutableList.of("fill", "line", "symbol", "circle", "heatmap", "fill-extrusion", "raster", "hillshade", "background");

        for (int i = 0; i < size; i++) {
            JsonNode idNode = layers.get(i)
                                    .get("id");
            JsonNode typeNode = layers.get(i)
                                      .get("type");
            if (idNode == null) {
                throw new BadRequestException("A layer in the Mapbox Style document has no id.");
            }
            if (typeNode == null) {
                throw new BadRequestException("A layer in the Mapbox Style document has no type.");
            }
            if (!typeNode.isTextual()) {
                throw new BadRequestException("A layer in the Mapbox Style document has an invalid type value (not a text).");
            }
            if (!idNode.isTextual()) {
                throw new BadRequestException("A layer in the Mapbox Style document has an invalid id value (not a text).");
            }
            String id = idNode.textValue();
            String type = typeNode.textValue();

            if (ids.contains(id)) {
                throw new BadRequestException("A layer in the Mapbox Style document has a duplicate id: " + id);
            }
            if (!types.contains(type)) {
                throw new BadRequestException("A layer in the Mapbox Style document has an invalid type: " + type);
            }
            ids.add(id);
        }
    }

    /**
     * checks if the request body from the PUT-Request is valid json
     *
     * @param requestBody the new Style as a String
     * @return the request body as Json Node, if json is valid
     */
    public static JsonNode validateRequestBodyMbStyle(byte[] requestBody, boolean validate) { //TODO change tests

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode requestBodyNode;
        try {
            requestBodyNode = objectMapper.readTree(requestBody);
            if (validate) {
                // parse into stylesheet schema
                MbStyleStylesheet stylesheet = objectMapper.treeToValue(requestBodyNode, MbStyleStylesheet.class);
                // additional checks
                validateRequestBody(requestBodyNode);
            }
        } catch (Exception e) {
            throw new BadRequestException(e);
        }

        return requestBodyNode;
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

    private static void validateRequestBodyMbStyle(byte[] requestBody, boolean validate, URL xsdPath){

        if (validate) {
            try {
                SchemaFactory factory =
                        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = factory.newSchema(xsdPath);
                Validator validator = schema.newValidator();
                validator.validate(new StreamSource(ByteSource.wrap(requestBody).openStream()));
            } catch (IOException | SAXException e) {
                throw new BadRequestException(e);
            }
        }
    }
}
