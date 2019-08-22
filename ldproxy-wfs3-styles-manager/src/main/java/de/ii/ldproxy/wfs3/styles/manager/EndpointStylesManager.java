/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles.manager;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiEndpointExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.wfs3.styles.ImmutableStyleMetadata;
import de.ii.ldproxy.wfs3.styles.ImmutableStyleSheet;
import de.ii.ldproxy.wfs3.styles.StyleFormatExtension;
import de.ii.ldproxy.wfs3.styles.StyleFormatMbStyles;
import de.ii.ldproxy.wfs3.styles.StyleMetadata;
import de.ii.ldproxy.wfs3.styles.StyleSheet;
import de.ii.ldproxy.wfs3.styles.StylesConfiguration;
import de.ii.ldproxy.wfs3.styles.StylesLinkGenerator;
import de.ii.ldproxy.wfs3.styles.StylesStore;
import de.ii.xtraplatform.auth.api.User;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * creates, updates and deletes a style from the service
 */
@Component
@Provides
@Instantiate
public class EndpointStylesManager implements OgcApiEndpointExtension, ConformanceClass {

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("styles")
            .addMethods(HttpMethods.POST, HttpMethods.PUT, HttpMethods.DELETE, HttpMethods.PATCH)
            .build();


    private final StylesStore stylesStore;
    private final OgcApiExtensionRegistry extensionRegistry;

    public EndpointStylesManager(@Requires StylesStore stylesStore,
                                 @Requires OgcApiExtensionRegistry extensionRegistry) {
        this.stylesStore = stylesStore;
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/t15/opf-styles-1/1.0/conf/manage-styles";
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData dataset) {
        return getStyleFormatStream(dataset).map(StyleFormatExtension::getMediaType)
                                            .collect(ImmutableSet.toImmutableSet());
    }

    private Stream<StyleFormatExtension> getStyleFormatStream(OgcApiDatasetData dataset) {
        return extensionRegistry.getExtensionsForType(StyleFormatExtension.class)
                                .stream()
                                .filter(styleFormatExtension -> styleFormatExtension.isEnabledForDataset(dataset));
    }

    @Override
    public boolean isEnabledForDataset(OgcApiDatasetData datasetData) {
        Optional<StylesConfiguration> stylesExtension = getExtensionConfiguration(datasetData, StylesConfiguration.class);

        if (stylesExtension.isPresent() &&
                stylesExtension.get()
                               .getManagerEnabled()) {
            return true;
        }
        return false;
    }

    /**
     * creates a new style
     *
     * @return empty response (201), with Location header
     */
    @Path("/")
    @POST
    @Consumes(StyleFormatMbStyles.MEDIA_TYPE_STRING)
    public Response postStyle(@Auth Optional<User> optionalUser, @Context OgcApiDataset dataset,
                              @Context OgcApiRequestContext ogcApiRequest, @Context HttpServletRequest request,
                              byte[] requestBody) {

        checkAuthorization(dataset.getData(), optionalUser);

        String datasetId = dataset.getId();
        StyleFormatExtension format = new StyleFormatMbStyles();

        // TODO: update
        JsonNode requestBodyJson = validateRequestBodyJSON(requestBody);
        if (requestBodyJson == null || !validateRequestBody(requestBodyJson)) {
            throw new BadRequestException();
        }

        String styleId = requestBodyJson.get("name")
                                        .asText();

        Pattern styleNamePattern = Pattern.compile("[^a-z0-9\\-_]", Pattern.CASE_INSENSITIVE);
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

        writeStylesheet(dataset.getData(), ogcApiRequest, styleId, format, requestBody, true);

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
     * updates one specific style
     *
     * @param styleId the local identifier of a specific style
     * @return empty response (204)
     */
    @Path("/{styleId}")
    @PUT
    @Consumes(StyleFormatMbStyles.MEDIA_TYPE_STRING)
    public Response putStyle(@Auth Optional<User> optionalUser, @PathParam("styleId") String styleId,
                             @Context OgcApiDataset dataset, @Context OgcApiRequestContext ogcApiRequest,
                             @Context HttpServletRequest request, byte[] requestBody) {

        checkAuthorization(dataset.getData(), optionalUser);

        StyleFormatExtension format = new StyleFormatMbStyles();

        JsonNode requestBodyJson = validateRequestBodyJSON(requestBody);

        if (requestBodyJson == null || !validateRequestBody(requestBodyJson))
            throw new BadRequestException();

        boolean newStyle = isNewStyle(dataset.getId(), styleId);
        writeStylesheet(dataset.getData(), ogcApiRequest, styleId, format, requestBody, newStyle);

        // TODO: add stylesheet to metadata, if newStyle == false

        return Response.noContent()
                       .build();
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
                                     @Context OgcApiDataset dataset, @Context OgcApiRequestContext ogcApiRequest,
                                     @Context HttpServletRequest request, byte[] requestBody) {

        checkAuthorization(dataset.getData(), optionalUser);

        boolean newStyle = isNewStyle(dataset.getId(), styleId);
        if (newStyle) {
            throw new NotFoundException();
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
            throw new BadRequestException(e.getMessage());
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
                                       @Context OgcApiDataset dataset, @Context OgcApiRequestContext ogcApiRequest,
                                       @Context HttpServletRequest request, byte[] requestBody) {

        checkAuthorization(dataset.getData(), optionalUser);

        boolean newStyle = isNewStyle(dataset.getId(), styleId);
        if (newStyle) {
            throw new NotFoundException();
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
            currentMetadata = mapper.readValue(stylesStore.get(styleId + ".metadata", dataset.getId()), new TypeReference<LinkedHashMap>() {
            });
            ObjectReader objectReader = mapper.readerForUpdating(currentMetadata);
            updatedMetadata = objectReader.readValue(requestBody);
            byte[] updatedMetadataString = mapper.writerWithDefaultPrettyPrinter()
                                                 .writeValueAsBytes(updatedMetadata); // TODO: remove pretty print
            putStyleDocument(dataset.getId(), styleId, "metadata", updatedMetadataString);
        } catch (IOException e) {
            throw new BadRequestException(e.getMessage());
        }

        return Response.noContent()
                       .build();
    }

    private boolean isNewStyle(String datasetId, String styleId) {

        return !stylesStore.has(styleId + ".metadata", datasetId);
    }

    private void writeStylesheet(OgcApiDatasetData datasetData, OgcApiRequestContext ogcApiRequest, String styleId,
                                 StyleFormatExtension format, byte[] requestBody, boolean newStyle) {

        String datasetId = datasetData.getId();

        try {
            putStyleDocument(datasetId, styleId, format.getFileExtension(), requestBody);

            if (newStyle) {
                final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

                ImmutableStyleSheet.Builder stylesheet = ImmutableStyleSheet.builder()
                                                                            .native_(true)
                                                                            .link(stylesLinkGenerator.generateStylesheetLink(ogcApiRequest.getUriCustomizer(),
                                                                                    styleId, format.getMediaType()))
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
                                @Context OgcApiDataset dataset) {

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
        for (String key : stylesStore.ids(datasetId)) {
            if (key.substring(0, key.lastIndexOf("."))
                   .equals(styleId)) {
                styleFound = true;
                stylesStore.delete(key, datasetId);
            }
        }
        if (!styleFound) {
            throw new NotFoundException();
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
        stylesStore.put(key, payload, datasetId);
    }

    /**
     * checks if the request body from the PUT-Request has valid content
     *
     * @param requestBody the new Style as a JsonNode
     * @return true if content is valid
     */
    public static boolean validateRequestBody(JsonNode requestBody) {

        // TODO: review
        JsonNode version = requestBody.get("version");
        JsonNode sources = requestBody.get("sources");
        JsonNode layers = requestBody.get("layers");


        if (layers == null || version == null || (version.isInt() && version.intValue() != 8) || sources == null) {
            return false;
        }
        int size = layers.size();
        List<String> ids = new ArrayList<>();
        List<String> types = ImmutableList.of("fill", "line", "symbol", "circle", "heatmap", "fill-extrusion", "raster", "hillshade", "background");

        for (int i = 0; i < size; i++) {
            JsonNode idNode = layers.get(i)
                                    .get("id");
            JsonNode typeNode = layers.get(i)
                                      .get("type");
            if (idNode == null || typeNode == null || !typeNode.isTextual() || !idNode.isTextual()) {
                return false;
            }
            String id = idNode.textValue();
            String type = typeNode.textValue();

            if (ids.contains(id) || !types.contains(type)) {
                return false;
            }
            ids.add(id);
        }

        return true;
    }

    /**
     * checks if the request body from the PUT-Request is valid json
     *
     * @param requestBody the new Style as a String
     * @return the request body as Json Node, if json is valid
     */
    public static JsonNode validateRequestBodyJSON(byte[] requestBody) { //TODO change tests

        // TODO: review
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
        JsonNode requestBodyNode;
        try {
            requestBodyNode = objectMapper.readTree(requestBody);

        } catch (Exception e) {
            return null;
        }

        return requestBodyNode;
    }
}
