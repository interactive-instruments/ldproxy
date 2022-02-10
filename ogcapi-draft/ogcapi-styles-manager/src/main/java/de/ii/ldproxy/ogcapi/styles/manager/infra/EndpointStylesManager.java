/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.manager.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiHeader;
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
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableQueryInputStyle;
import de.ii.ldproxy.ogcapi.styles.domain.StyleRepository;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ldproxy.ogcapi.styles.manager.domain.ImmutableQueryInputStyleCreateReplace;
import de.ii.ldproxy.ogcapi.styles.manager.domain.ImmutableQueryInputStyleDelete;
import de.ii.ldproxy.ogcapi.styles.manager.domain.QueriesHandlerStylesManager;
import de.ii.xtraplatform.auth.domain.User;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * creates, updates and deletes a style from the service
 */
@Component
@Provides
@Instantiate
public class EndpointStylesManager extends Endpoint implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStylesManager.class);
    private static final List<String> TAGS = ImmutableList.of("Create, update and delete styles");

    private final QueriesHandlerStylesManager queryHandler;

    public EndpointStylesManager(@Requires ExtensionRegistry extensionRegistry,
                                 @Requires QueriesHandlerStylesManager queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
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
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        Optional<StylesConfiguration> stylesExtension = apiData.getExtension(StylesConfiguration.class);
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("styles")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_STYLES_MANAGER);
        String path = "/styles";
        List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path, HttpMethods.POST);
        List<ApiHeader> headers = getHeaders(extensionRegistry, apiData, path, HttpMethods.POST);
        String operationSummary = "add a new style";
        String description = "Adds a style to the style repository";
        if (stylesExtension.isPresent() && stylesExtension.get().getValidationEnabled()) {
            description += " or just validates a style.\n" +
                    "If the header `Prefer` is set to `handling=strict`, the style will be validated before adding " +
                    "the style to the server. If the parameter `dry-run` is set to `true`, the server will " +
                    "not be changed and only any validation errors will be reported";
        }
        description += ".\n" +
                "If a new style is created, the following rules apply:\n";
        if (stylesExtension.isPresent() && stylesExtension.get().getUseIdFromStylesheet()) {
            description += "* If the style submitted in the request body includes an identifier (this depends on " +
                    "the style encoding), that identifier will be used. If a style with that identifier " +
                    "already exists, an error is returned.\n" +
                    "* If no identifier can be determined from the submitted style, the server will assign " +
                    "a new identifier to the style.\n";
        } else {
            description += "* The server will assign a new identifier to the style.\n";
        }
        description += "* A minimal style metadata resource is created at `/styles/{styleId}/metadata`. " +
                "You can update the metadata using a PUT request to keep the style metadata consistent with " +
                "the style definition.\n" +
                "* The URI of the new style is returned in the header `Location`.\n";
        Optional<String> operationDescription = Optional.of(description);
        ImmutableOgcApiResourceData.Builder resourceBuilder = new ImmutableOgcApiResourceData.Builder()
                .path(path);
        Map<MediaType, ApiMediaTypeContent> requestContent = getRequestContent(apiData, path, HttpMethods.POST);
        ApiOperation operation = addOperation(apiData, HttpMethods.POST, requestContent, queryParameters, headers, path, operationSummary, operationDescription, Optional.empty(), TAGS);
        if (operation!=null)
            resourceBuilder.putOperations("POST", operation);
        definitionBuilder.putResources(path, resourceBuilder.build());
        path = "/styles/{styleId}";
        ImmutableList<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
        queryParameters = getQueryParameters(extensionRegistry, apiData, path, HttpMethods.PUT);
        headers = getHeaders(extensionRegistry, apiData, path, HttpMethods.PUT);
        operationSummary = "replace a style";
        description = "Replace an existing style with the id `styleId` ";
        if (stylesExtension.isPresent() && stylesExtension.get().getValidationEnabled()) {
            description += " or just validate a style.\n" +
                    "If the header `Prefer` is set to `handling=strict`, the style will be validated before adding " +
                    "the style to the server. If the parameter `dry-run` is set to `true`, the server will " +
                    "not be changed and only any validation errors will be reported";
        }
        description += ".\nThe style metadata resource at `/styles/{styleId}/metadata` " +
                "is not updated.";
        operationDescription = Optional.of(description);
        resourceBuilder = new ImmutableOgcApiResourceData.Builder()
                .path(path)
                .pathParameters(pathParameters);
        requestContent = getRequestContent(apiData, path, HttpMethods.PUT);
        operation = addOperation(apiData, HttpMethods.PUT, requestContent, queryParameters, headers, path, operationSummary, operationDescription, Optional.empty(), TAGS);
        if (operation!=null)
            resourceBuilder.putOperations("PUT", operation);
        queryParameters = getQueryParameters(extensionRegistry, apiData, path, HttpMethods.DELETE);
        headers = getHeaders(extensionRegistry, apiData, path, HttpMethods.DELETE);
        operationSummary = "delete a style";
        operationDescription = Optional.of("Delete an existing style with the id `styleId`. " +
                "Deleting a style also deletes the subordinate resources, i.e., the style metadata.");
        requestContent = getRequestContent(apiData, path, HttpMethods.DELETE);
        operation = addOperation(apiData, HttpMethods.DELETE, requestContent, queryParameters, headers, path, operationSummary, operationDescription, Optional.empty(), TAGS);
        if (operation!=null)
            resourceBuilder.putOperations("DELETE", operation);
        definitionBuilder.putResources(path, resourceBuilder.build());

        return definitionBuilder.build();
    }

    /**
     * creates a new style
     *
     * @return empty response (201), with Location header
     */
    @Path("/")
    @POST
    public Response postStyle(@Auth Optional<User> optionalUser,
                              @DefaultValue("false") @QueryParam("dry-run") boolean dryRun,
                              @Context OgcApi api,
                              @Context ApiRequestContext requestContext,
                              @Context HttpServletRequest request,
                              byte[] requestBody) {

        OgcApiDataV2 apiData = api.getData();
        checkAuthorization(apiData, optionalUser);

        QueriesHandlerStylesManager.QueryInputStyleCreateReplace queryInput = new ImmutableQueryInputStyleCreateReplace.Builder()
                .contentType(mediaTypeFromString(request.getContentType()))
                .requestBody(requestBody)
                .strict(strictHandling(request.getHeaders("Prefer")))
                .dryRun(dryRun)
                .build();

        return queryHandler.handle(QueriesHandlerStylesManager.Query.CREATE_STYLE, queryInput, requestContext);
    }

    /**
     * creates or updates a style(sheet)
     *
     * @param styleId the local identifier of a specific style
     * @return empty response (204)
     */
    @Path("/{styleId}")
    @PUT
    public Response putStyle(@Auth Optional<User> optionalUser,
                             @PathParam("styleId") String styleId,
                             @DefaultValue("false") @QueryParam("dry-run") boolean dryRun,
                             @Context OgcApi api,
                             @Context ApiRequestContext requestContext,
                             @Context HttpServletRequest request,
                             byte[] requestBody) {

        OgcApiDataV2 apiData = api.getData();
        checkAuthorization(apiData, optionalUser);
        checkPathParameter(extensionRegistry, apiData, "/styles/{styleId}", "styleId", styleId);

        QueriesHandlerStylesManager.QueryInputStyleCreateReplace queryInput = new ImmutableQueryInputStyleCreateReplace.Builder()
                .styleId(styleId)
                .contentType(mediaTypeFromString(request.getContentType()))
                .requestBody(requestBody)
                .strict(strictHandling(request.getHeaders("Prefer")))
                .dryRun(dryRun)
                .build();

        return queryHandler.handle(QueriesHandlerStylesManager.Query.REPLACE_STYLE, queryInput, requestContext);
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
                                @Context OgcApi api, @Context ApiRequestContext requestContext) {

        OgcApiDataV2 apiData = api.getData();
        checkAuthorization(apiData, optionalUser);
        checkPathParameter(extensionRegistry, apiData, "/styles/{styleId}", "styleId", styleId);

        QueriesHandlerStylesManager.QueryInputStyleDelete queryInput = new ImmutableQueryInputStyleDelete.Builder()
                .styleId(styleId)
                .build();

        return queryHandler.handle(QueriesHandlerStylesManager.Query.DELETE_STYLE, queryInput, requestContext);
    }
}
