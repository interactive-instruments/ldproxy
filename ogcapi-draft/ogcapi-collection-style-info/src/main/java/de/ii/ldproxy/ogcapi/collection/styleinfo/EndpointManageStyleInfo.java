/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collection.styleinfo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * creates, updates and deletes a style from the service
 */
@Component
@Provides
@Instantiate
public class EndpointManageStyleInfo extends OgcApiEndpointSubCollection implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointManageStyleInfo.class);

    private static final List<String> TAGS = ImmutableList.of("Mutate data collections");

    private final File styleInfosStore;

    public EndpointManageStyleInfo(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                                   @Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
        this.styleInfosStore = new File(bundleContext.getProperty(DATA_DIR_KEY) + File.separator + "styleInfos");
        if (!styleInfosStore.exists()) {
            styleInfosStore.mkdirs();
        }
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/t15/opf-styles-1/1.0/conf/style-info");
    }

    @Override
    protected Class getConfigurationClass() {
        return StyleInfoConfiguration.class;
    }

    @Override
    public OgcApiEndpointDefinition getDefinition(OgcApiApiDataV2 apiData) {
        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableOgcApiEndpointDefinition.Builder definitionBuilder = new ImmutableOgcApiEndpointDefinition.Builder()
                    .apiEntrypoint("collections")
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_STYLE_INFO);
            String path = "/collections/{collectionId}";
            List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
            Optional<OgcApiPathParameter> optCollectionIdParam = pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
            if (!optCollectionIdParam.isPresent()) {
                LOGGER.error("Path parameter 'collectionId' missing for resource at path '" + path + "'. The resource will not be available.");
            } else {
                final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
                final boolean explode = collectionIdParam.getExplodeInOpenApi();
                final Set<String> collectionIds = (explode) ?
                        collectionIdParam.getValues(apiData) :
                        ImmutableSet.of("{collectionId}");
                for (String collectionId : collectionIds) {
                    final List<OgcApiQueryParameter> queryParameters = explode ?
                            getQueryParameters(extensionRegistry, apiData, path, collectionId, HttpMethods.PATCH) :
                            getQueryParameters(extensionRegistry, apiData, path, HttpMethods.PATCH);
                    final String operationSummary = "update the information about available styles for the feature collection '" + collectionId + "'";
                    Optional<String> operationDescription = Optional.of("The content of the request may include an updated list of styles and/or an update to the default style.");
                    String resourcePath = "/collections/" + collectionId;
                    ImmutableOgcApiResourceData.Builder resourceBuilder = new ImmutableOgcApiResourceData.Builder()
                            .path(resourcePath)
                            .pathParameters(pathParameters);
                    // TODO secure the PATCH operation and remove hide=true
                    OgcApiOperation operation = addOperation(apiData, OgcApiContext.HttpMethods.PATCH, queryParameters, collectionId, "", operationSummary, operationDescription, TAGS, true);
                    if (operation!=null)
                        resourceBuilder.putOperations("PATCH", operation);
                    definitionBuilder.putResources(resourcePath, resourceBuilder.build());
                }
            }
            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
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
                                       @Context OgcApiApi dataset, @Context OgcApiRequestContext ogcApiRequest,
                                       @Context HttpServletRequest request, byte[] requestBody) {

        checkAuthorization(dataset.getData(), optionalUser);
        checkCollectionExists(dataset.getData(), collectionId);

        CollectionStyleInfoFormatExtension outputFormat = getFormats().stream()
                .filter(format -> format.getMediaType().matches(ogcApiRequest.getMediaType().type()))
                .map(CollectionStyleInfoFormatExtension.class::cast)
                .findAny()
                .orElseThrow(() -> new NotSupportedException(MessageFormat.format("The provided media type ''{0}'' is not supported for this resource.", ogcApiRequest.getMediaType())));

        try {
            outputFormat.patchStyleInfos(requestBody, styleInfosStore, dataset, collectionId);
        } catch (IOException e) {
            throw new BadRequestException(e);
        }

        return Response.noContent()
                       .build();
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(CollectionStyleInfoFormatExtension.class)
                .stream()
                .filter(CollectionStyleInfoFormatExtension::canSupportTransactions)
                .collect(Collectors.toList());
        return formats;
    }
}
