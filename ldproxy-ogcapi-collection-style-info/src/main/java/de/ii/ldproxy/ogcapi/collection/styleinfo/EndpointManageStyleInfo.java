/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collection.styleinfo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext.HttpMethods;
import de.ii.xtraplatform.auth.api.User;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * creates, updates and deletes a style from the service
 */
@Component
@Provides
@Instantiate
public class EndpointManageStyleInfo implements OgcApiEndpointExtension, ConformanceClass {

    @Requires
    I18n i18n;

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("collections")
            .addMethods(HttpMethods.PATCH)
            .subPathPattern("^/?[\\w\\-]+/?$")
            .build();

    private final OgcApiExtensionRegistry extensionRegistry;
    private final File styleInfosStore;

    public EndpointManageStyleInfo(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                                   @Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
        this.styleInfosStore = new File(bundleContext.getProperty(DATA_DIR_KEY) + File.separator + "styleInfos");
        if (!styleInfosStore.exists()) {
            styleInfosStore.mkdirs();
        }
    }

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/t15/opf-styles-1/1.0/conf/style-info";
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData dataset, String subPath) {
        return ImmutableSet.of(
                new ImmutableOgcApiMediaType.Builder()
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build());
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        Optional<StyleInfoConfiguration> styleInfoExtension = getExtensionConfiguration(apiData, StyleInfoConfiguration.class);

        if (styleInfoExtension.isPresent() &&
                styleInfoExtension.get()
                        .getEnabled()) {
            return true;
        }
        return false;
    }

    /**
     * partial update to the metadata of a style
     *
     * @param collectionId the identifier of the collection
     * @return empty response (204)
     */
    @Path("/{collectionId}")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    public Response patchStyleMetadata(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId,
                                       @Context OgcApiDataset dataset, @Context OgcApiRequestContext ogcApiRequest,
                                       @Context HttpServletRequest request, byte[] requestBody) {

        checkAuthorization(dataset.getData(), optionalUser);
        checkCollectionId(collectionId);

        final String apiId = dataset.getData().getId();
        File apiDir = new File(styleInfosStore + File.separator + apiId);
        if (!apiDir.exists()) {
            apiDir.mkdirs();
        }

        boolean newStyleInfos = isNewStyleInfo(dataset.getId(), collectionId);

        // prepare Jackson mapper for deserialization
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            ObjectReader objectReader;
            StyleInfos updatedContent;
            if (newStyleInfos) {
                updatedContent = mapper.readValue(requestBody, StyleInfos.class);
            } else {
                /* TODO currently treat it like a put, change to proper PATCH
                mapper.readValue(requestBody, StyleInfos.class);
                File metadataFile = new File( styleInfosStore + File.separator + dataset.getId() + File.separator + collectionId);
                StyleInfos currentContent = mapper.readValue(metadataFile, StyleInfos.class);
                objectReader = mapper.readerForUpdating(currentContent);
                updatedContent = objectReader.readValue(requestBody);
                 */
                updatedContent = mapper.readValue(requestBody, StyleInfos.class);
            }
            // parse input for validation
            byte[] updatedContentString = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(updatedContent); // TODO: remove pretty print
            putStylesInfoDocument(dataset.getId(), collectionId, updatedContentString);
        } catch (IOException e) {
            throw new BadRequestException(e.getMessage());
        }

        return Response.noContent()
                       .build();
    }

    private boolean isNewStyleInfo(String datasetId, String collectionId) {

        File file = new File( styleInfosStore + File.separator + datasetId + File.separator + collectionId);
        return !file.exists();
    }

    /**
     * search for the style in the store and update it, or create a new one
     *  @param datasetId       the dataset id
     * @param collectionId           the id of the collection, for which a styleInfos document should be updated or created
     * @param payload the new Style as a byte array
     */
    private void putStylesInfoDocument(String datasetId, String collectionId, byte[] payload) {

        checkCollectionId(collectionId);
        File styleFile = new File(styleInfosStore + File.separator + datasetId + File.separator + collectionId);
        try {
            Files.write(styleFile.toPath(), payload);
        } catch (IOException e) {
            throw new ServerErrorException("could not PATCH style information: "+collectionId, 500);
        }
    }

    private static void checkCollectionId(String collectionId) {
        if (!collectionId.matches("[\\w\\-]+")) {
            throw new BadRequestException("Only character 0-9, A-Z, a-z, dash and underscore are allowed in a collection identifier. Found: "+collectionId);
        }
    }
}
