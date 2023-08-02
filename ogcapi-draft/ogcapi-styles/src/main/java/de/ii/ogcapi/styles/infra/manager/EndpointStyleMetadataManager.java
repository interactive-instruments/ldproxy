/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.infra.manager;

import static de.ii.ogcapi.styles.domain.QueriesHandlerStyles.SCOPE_STYLES_WRITE;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiHeader;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.styles.domain.StyleMetadataFormatExtension;
import de.ii.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ogcapi.styles.domain.manager.ImmutableQueryInputStyleMetadata;
import de.ii.ogcapi.styles.domain.manager.QueriesHandlerStylesManager;
import de.ii.xtraplatform.auth.domain.User;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @title Style Metadata
 * @path styles/{styleId}/metadata
 * @langEn Update style metadata.
 * @langDe Aktualisieren der Metadaten eines Styles.
 * @ref:formats {@link de.ii.ogcapi.styles.domain.StyleMetadataFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointStyleMetadataManager extends Endpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStyleMetadataManager.class);
  private static final List<String> TAGS = ImmutableList.of("Create, update and delete styles");

  private final QueriesHandlerStylesManager queryHandler;

  @Inject
  public EndpointStyleMetadataManager(
      ExtensionRegistry extensionRegistry, QueriesHandlerStylesManager queryHandler) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    Optional<StylesConfiguration> extension = apiData.getExtension(StylesConfiguration.class);

    return extension
        .filter(StylesConfiguration::isEnabled)
        .filter(StylesConfiguration::isManagerEnabled)
        .isPresent();
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return StylesConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getResourceFormats() {
    if (formats == null)
      formats =
          extensionRegistry.getExtensionsForType(StyleMetadataFormatExtension.class).stream()
              .filter(StyleMetadataFormatExtension::canSupportTransactions)
              .collect(Collectors.toList());
    return formats;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("styles")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_STYLE_METADATA_MANAGER);
    String path = "/styles/{styleId}/metadata";
    HttpMethods methodReplace = HttpMethods.PUT;
    List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
    List<OgcApiQueryParameter> queryParameters =
        getQueryParameters(extensionRegistry, apiData, path, methodReplace);
    List<ApiHeader> headers = getHeaders(extensionRegistry, apiData, path, methodReplace);
    String operationSummary = "update the metadata document of a style";
    Optional<String> operationDescription =
        Optional.of(
            "Update the style metadata for the style with the id `styleId`. "
                + "This operation updates the complete metadata document.");
    ImmutableOgcApiResourceData.Builder resourceBuilder =
        new ImmutableOgcApiResourceData.Builder().path(path).pathParameters(pathParameters);
    Map<MediaType, ApiMediaTypeContent> requestContent = getRequestContent(apiData);
    ApiOperation.of(
            path,
            methodReplace,
            requestContent,
            queryParameters,
            headers,
            operationSummary,
            operationDescription,
            Optional.empty(),
            getOperationId("replaceStyleMetadata"),
            SCOPE_STYLES_WRITE,
            TAGS)
        .ifPresent(operation -> resourceBuilder.putOperations(methodReplace.name(), operation));
    HttpMethods methodUpdate = HttpMethods.PATCH;
    queryParameters = getQueryParameters(extensionRegistry, apiData, path, methodUpdate);
    headers = getHeaders(extensionRegistry, apiData, path, methodUpdate);
    operationSummary = "update parts of the style metadata";
    operationDescription =
        Optional.of(
            "Update selected elements of the style metadata for "
                + "the style with the id `styleId`.\n"
                + "The PATCH semantics in this operation are defined by "
                + "RFC 7396 (JSON Merge Patch). From the specification:\n"
                + "\n"
                + "_'A JSON merge patch document describes changes to be "
                + "made to a target JSON document using a syntax that "
                + "closely mimics the document being modified. Recipients "
                + "of a merge patch document determine the exact set of "
                + "changes being requested by comparing the content of "
                + "the provided patch against the current content of the "
                + "target document. If the provided merge patch contains "
                + "members that do not appear within the target, those "
                + "members are added. If the target does contain the "
                + "member, the value is replaced. Null values in the "
                + "merge patch are given special meaning to indicate "
                + "the removal of existing values in the target.'_\n"
                + "\n"
                + "Some examples:\n"
                + "\n"
                + "To add or update the point of contact, the access"
                + "constraint and the revision date, just send\n"
                + "\n"
                + "```\n"
                + "{\n"
                + "  \"pointOfContact\": \"Jane Doe\",\n"
                + "  \"accessConstraints\": \"restricted\",\n"
                + "  \"dates\": {\n"
                + "    \"revision\": \"2019-05-17T11:46:12Z\"\n"
                + "  }\n"
                + "}\n"
                + "```\n"
                + "\n"
                + "To remove the point of contact, the access "
                + "constraint and the revision date, send \n"
                + "\n"
                + "```\n"
                + "{\n"
                + "  \"pointOfContact\": null,\n"
                + "  \"accessConstraints\": null,\n"
                + "  \"dates\": {\n"
                + "    \"revision\": null\n"
                + "  }\n"
                + "}\n"
                + "```\n"
                + "\n"
                + "For arrays the complete array needs to be sent. "
                + "To add a keyword to the example style metadata object, send\n"
                + "\n"
                + "```\n"
                + "{\n"
                + "  \"keywords\": [ \"basemap\", \"TDS\", \"TDS 6.1\", \"OGC API\", \"new keyword\" ]\n"
                + "}\n"
                + "```\n"
                + "\n"
                + "To remove the \"TDS\" keyword, send\n"
                + "\n"
                + "```\n"
                + "{\n"
                + "  \"keywords\": [ \"basemap\", \"TDS 6.1\", \"OGC API\", \"new keyword\" ]\n"
                + "}\n"
                + "```\n"
                + "\n"
                + "To remove the keywords, send\n"
                + "\n"
                + "```\n"
                + "{\n"
                + "  \"keywords\": null\n"
                + "}\n"
                + "```\n"
                + "\n"
                + "The same applies to `stylesheets` and `layers`. To update "
                + "these members, you have to send the complete new array value.");
    requestContent = getRequestContent(apiData);
    ApiOperation.of(
            path,
            methodUpdate,
            requestContent,
            queryParameters,
            headers,
            operationSummary,
            operationDescription,
            Optional.empty(),
            getOperationId("updateStyleMetadata"),
            SCOPE_STYLES_WRITE,
            TAGS)
        .ifPresent(operation -> resourceBuilder.putOperations(methodUpdate.name(), operation));
    definitionBuilder.putResources(path, resourceBuilder.build());

    return definitionBuilder.build();
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
  public Response putStyleMetadata(
      @Auth Optional<User> optionalUser,
      @PathParam("styleId") String styleId,
      @DefaultValue("false") @QueryParam("dry-run") boolean dryRun,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @Context HttpServletRequest request,
      byte[] requestBody) {

    OgcApiDataV2 apiData = api.getData();
    checkPathParameter(
        extensionRegistry, apiData, "/styles/{styleId}/metadata", "styleId", styleId);

    QueriesHandlerStylesManager.QueryInputStyleMetadata queryInput =
        new ImmutableQueryInputStyleMetadata.Builder()
            .styleId(styleId)
            .contentType(mediaTypeFromString(request.getContentType()))
            .requestBody(requestBody)
            .strict(strictHandling(request.getHeaders("Prefer")))
            .dryRun(dryRun)
            .build();

    return queryHandler.handle(
        QueriesHandlerStylesManager.Query.REPLACE_STYLE_METADATA, queryInput, requestContext);
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
  public Response patchStyleMetadata(
      @Auth Optional<User> optionalUser,
      @PathParam("styleId") String styleId,
      @DefaultValue("false") @QueryParam("dry-run") boolean dryRun,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @Context HttpServletRequest request,
      byte[] requestBody) {

    OgcApiDataV2 apiData = api.getData();
    checkPathParameter(
        extensionRegistry, apiData, "/styles/{styleId}/metadata", "styleId", styleId);

    QueriesHandlerStylesManager.QueryInputStyleMetadata queryInput =
        new ImmutableQueryInputStyleMetadata.Builder()
            .styleId(styleId)
            .contentType(mediaTypeFromString(request.getContentType()))
            .requestBody(requestBody)
            .strict(strictHandling(request.getHeaders("Prefer")))
            .dryRun(dryRun)
            .build();

    return queryHandler.handle(
        QueriesHandlerStylesManager.Query.UPDATE_STYLE_METADATA, queryInput, requestContext);
  }
}
