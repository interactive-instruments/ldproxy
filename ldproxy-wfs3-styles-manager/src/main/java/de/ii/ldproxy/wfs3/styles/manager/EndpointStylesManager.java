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
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.*;
import de.ii.ldproxy.wfs3.styles.*;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.kvstore.api.KeyNotFoundException;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;
import de.ii.xtraplatform.kvstore.api.Transaction;
import de.ii.xtraplatform.kvstore.api.WriteTransaction;
import de.ii.xtraplatform.service.api.Service;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * creates, updates and deletes a style from the service
 */
@Component
@Provides
@Instantiate
public class EndpointStylesManager implements Wfs3EndpointExtension, Wfs3ConformanceClass {

    @Requires
    private KeyValueStore keyValueStore;

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/t15/opf-styles-1/1.0/conf/manage-styles";
    }

    @Override
    public boolean isConformanceEnabledForService(Wfs3ServiceData serviceData) {
        return isEnabledForService(serviceData);
    }

    @Override
    public String getPath() {
        return "styles";
    }

    @Override
    public List<String> getMethods() {
        return ImmutableList.of("POST", "PUT", "DELETE", "PATCH");
    }

    @Override
    public boolean isEnabledForService(Wfs3ServiceData serviceData) {
        Optional<StylesConfiguration> stylesExtension = getExtensionConfiguration(serviceData, StylesConfiguration.class);

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
     * @return  empty response (201), with Location header
     */
    @Path("/")
    @POST
    @Consumes(Wfs3MediaTypes.MBS)
    public Response postStyle(@Auth Optional<User> optionalUser, @Context Service service, @Context Wfs3RequestContext wfs3Request, @Context HttpServletRequest request, InputStream requestBody) {

        Wfs3Service wfs3Service = (Wfs3Service) service;
        Wfs3ServiceData serviceData = wfs3Service.getData();

        checkAuthorization(serviceData, optionalUser);

        StyleFormatExtension format = new StyleFormatMbStyles();

        Scanner s = new Scanner(requestBody).useDelimiter("\\A");
        String requestBodyString = s.hasNext() ? s.next() : "";

        // TODO: update
        JsonNode requestBodyJson = validateRequestBodyJSON(requestBodyString);
        if (requestBodyJson == null || !validateRequestBody(requestBodyJson))
            throw new BadRequestException();

        String styleId = requestBodyJson.get("name")
                .asText();

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                .getChildStore(service.getId());

        Pattern styleNamePattern = Pattern.compile("[^a-z0-9\\-_]", Pattern.CASE_INSENSITIVE);
        Matcher styleNameMatcher = styleNamePattern.matcher(styleId);
        if (!isNewStyle(stylesStore, styleId)) {
            throw new WebApplicationException(Response.Status.CONFLICT); // TODO
        } else if (styleId.contains(" ") || styleNameMatcher.find()) {
            int id = 0;
            while (!isNewStyle(stylesStore, Integer.toString(id))) {
                id++;
            }
            styleId = Integer.toString(id);
        }

        writeStylesheet(wfs3Service, wfs3Request, styleId, format, requestBodyString, true);

        // Return 201 with Location header
        URI newURI;
        try {
            newURI = wfs3Request.getUriCustomizer().copy().ensureLastPathSegment(styleId).build();
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
     * @return  empty response (204)
     */
    @Path("/{styleId}")
    @PUT
    @Consumes(Wfs3MediaTypes.MBS)
    public Response putStyle(@Auth Optional<User> optionalUser, @PathParam("styleId") String styleId, @Context Service service, @Context Wfs3RequestContext wfs3Request, @Context HttpServletRequest request, InputStream requestBody) {

        Wfs3Service wfs3Service = (Wfs3Service) service;
        Wfs3ServiceData serviceData = wfs3Service.getData();

        checkAuthorization(serviceData, optionalUser);

        StyleFormatExtension format = new StyleFormatMbStyles();

        Scanner s = new Scanner(requestBody).useDelimiter("\\A");
        String requestBodyString = s.hasNext() ? s.next() : "";

        JsonNode requestBodyJson = validateRequestBodyJSON(requestBodyString);

        if (requestBodyJson == null || !validateRequestBody(requestBodyJson))
            throw new BadRequestException();

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                .getChildStore(service.getId());

        boolean newStyle = isNewStyle(stylesStore, styleId);
        writeStylesheet(wfs3Service, wfs3Request, styleId, format, requestBodyString, newStyle);

        // TODO: add stylesheet to metadata, if newStyle == false

        return Response.noContent()
                       .build();
    }

    /**
     * updates the metadata document of a style
     *
     * @param styleId the local identifier of a specific style
     * @return  empty response (204)
     */
    @Path("/{styleId}/metadata")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putStyleMetadata(@Auth Optional<User> optionalUser, @PathParam("styleId") String styleId, @Context Service service, @Context Wfs3RequestContext wfs3Request, @Context HttpServletRequest request, InputStream requestBody) {

        Wfs3Service wfs3Service = (Wfs3Service) service;
        Wfs3ServiceData serviceData = wfs3Service.getData();

        checkAuthorization(serviceData, optionalUser);

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                .getChildStore(service.getId());

        boolean newStyle = isNewStyle(stylesStore, styleId);
        if (newStyle) {
            throw new NotFoundException();
        }

        Scanner s = new Scanner(requestBody).useDelimiter("\\A");
        String requestBodyString = s.hasNext() ? s.next() : "";

        // prepare Jackson mapper for deserialization
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            // parse input for validation
            mapper.readValue(requestBodyString, StyleMetadata.class);
            putStyleDocument(stylesStore, styleId, "metadata", requestBodyString);
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
     * @return  empty response (204)
     */
    @Path("/{styleId}/metadata")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    public Response patchStyleMetadata(@Auth Optional<User> optionalUser, @PathParam("styleId") String styleId, @Context Service service, @Context Wfs3RequestContext wfs3Request, @Context HttpServletRequest request, InputStream requestBody) {

        Wfs3Service wfs3Service = (Wfs3Service) service;
        Wfs3ServiceData serviceData = wfs3Service.getData();

        checkAuthorization(serviceData, optionalUser);

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                .getChildStore(service.getId());

        boolean newStyle = isNewStyle(stylesStore, styleId);
        if (newStyle) {
            throw new NotFoundException();
        }

        Scanner s = new Scanner(requestBody).useDelimiter("\\A");
        String requestBodyString = s.hasNext() ? s.next() : "";

        // prepare Jackson mapper for deserialization
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Map<String, Object> currentMetadata;
        Map<String, Object> updatedMetadata;
        try {
            // parse input for validation
            mapper.readValue(requestBodyString, StyleMetadata.class);
            currentMetadata = mapper.readValue(stylesStore.getValueReader(styleId+".metadata"), new TypeReference<LinkedHashMap>() {});
            ObjectReader objectReader = mapper.readerForUpdating(currentMetadata);
            updatedMetadata = objectReader.readValue(requestBodyString);
            String updatedMetadataString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(updatedMetadata); // TODO: remove pretty print
            putStyleDocument(stylesStore, styleId, "metadata", updatedMetadataString);
        } catch (IOException | KeyNotFoundException e) {
            throw new BadRequestException(e.getMessage());
        }

        return Response.noContent()
                .build();
    }

    private boolean isNewStyle(KeyValueStore stylesStore, String styleId) {

        return !stylesStore.containsKey(styleId + ".metadata");
    }

    private void writeStylesheet(Wfs3Service wfs3Service, Wfs3RequestContext wfs3Request, String styleId,
                                    StyleFormatExtension format, String requestBodyString, boolean newStyle) {

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles").getChildStore(wfs3Service.getId());

        try {
            putStyleDocument(stylesStore, styleId, format.getFileExtension(), requestBodyString);

            if (newStyle) {
                final StylesLinkGenerator stylesLinkGenerator = new StylesLinkGenerator();

                ImmutableStyleSheet.Builder stylesheet = ImmutableStyleSheet.builder()
                        .native_(true)
                        .link(stylesLinkGenerator.generateStylesheetLink(wfs3Request.getUriCustomizer(),
                                styleId, format.getMediaType().main().toString()))
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
                String metadataRequestBody = mapper.writeValueAsString(metadata.build());
                putStyleDocument(stylesStore, styleId, "metadata", metadataRequestBody);
            }
        }
        catch (Exception e) {
            // something went wrong, clean up
            deleteStyle(stylesStore, styleId);
            //
            throw new ServerErrorException(500); // TODO: details
        }
    }

    /**
     * deletes a style
     *
     * @param styleId the local identifier of a specific style
     * @return  empty response (204)
     */
    @Path("/{styleId}")
    @DELETE
    public Response deleteStyle(@Auth Optional<User> optionalUser, @PathParam("styleId") String styleId, @Context Service service) {

        checkAuthorization(((Wfs3Service) service).getData(), optionalUser);

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                                                 .getChildStore(service.getId());

        deleteStyle(stylesStore, styleId);

        return Response.noContent()
                       .build();
    }

    /**
     * search for the style in the store and delete it (i.e., all style documents)
     *
     * @param stylesStore the key value store
     * @param styleId     the id of the style, that should be deleted
     */
    private static void deleteStyle(KeyValueStore stylesStore, String styleId) {
        boolean styleFound = false;
        for (String key : stylesStore.getKeys()) {
            if (key.substring(0,key.lastIndexOf(".")).equals(styleId)) {
                styleFound = true;
                Transaction deleteTransaction = stylesStore.openDeleteTransaction(key);
                try {
                    deleteTransaction.execute(); //TODO should throw exception
                    deleteTransaction.commit();
                } catch (IOException e) {
                    deleteTransaction.rollback();
                } finally {
                    deleteTransaction.close();
                }
            }
        }
        if (!styleFound) {
            throw new NotFoundException();
        }
    }

    /**
     * search for the style in the store and update it, or create a new one
     *
     * @param stylesStore       the key value store
     * @param styleId           the id of the style, for which a document should be updated or created
     * @param fileExtension     the type of document
     * @param requestBodyString the new Style as a String
     */
    private static void putStyleDocument(KeyValueStore stylesStore, String styleId, String fileExtension, String requestBodyString) {

        String key = styleId + "." + fileExtension;
        WriteTransaction<String> transaction = stylesStore.openWriteTransaction(key);
        putTransaction(transaction, requestBodyString);
    }

    /**
     * a complete put transaction
     *
     * @param transaction       the write Transaction on the specific Style
     * @param requestBodyString the new Style as a String
     */
    private static void putTransaction(WriteTransaction<String> transaction, String requestBodyString) {
        try {
            transaction.write(requestBodyString);
            transaction.execute();
            transaction.commit();
        } catch (IOException e) {
            transaction.rollback();
        } finally {
            transaction.close();
        }
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
     * @param requestBodyString the new Style as a String
     * @return the request body as Json Node, if json is valid
     */
    public static JsonNode validateRequestBodyJSON(String requestBodyString) { //TODO change tests

        // TODO: review
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
        JsonNode requestBody;
        try {
            requestBody = objectMapper.readTree(requestBodyString);

        } catch (Exception e) {
            return null;
        }

        return requestBody;
    }
}
