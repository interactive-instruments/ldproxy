/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.manager.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiHeader;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
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
import de.ii.ogcapi.styles.manager.domain.ImmutableQueryInputStyleMetadata;
import de.ii.ogcapi.styles.manager.domain.QueriesHandlerStylesManager;
import de.ii.xtraplatform.auth.domain.User;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

/** update style metadata */
@Singleton
@AutoBind
public class EndpointStyleMetadataManagerCollection extends EndpointSubCollection {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(EndpointStyleMetadataManagerCollection.class);
  private static final List<String> TAGS = ImmutableList.of("Create, update and delete styles");

  private final QueriesHandlerStylesManager queryHandler;

  @Inject
  public EndpointStyleMetadataManagerCollection(
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
  public List<? extends FormatExtension> getFormats() {
    if (formats == null)
      formats =
          extensionRegistry.getExtensionsForType(StyleMetadataFormatExtension.class).stream()
              .filter(StyleMetadataFormatExtension::canSupportTransactions)
              .collect(Collectors.toList());
    return formats;
  }

  private Map<MediaType, ApiMediaTypeContent> getRequestContent(
      OgcApiDataV2 apiData, String path, HttpMethods method) {
    return getFormats().stream()
        .filter(outputFormatExtension -> outputFormatExtension.isEnabledForApi(apiData))
        .map(f -> f.getRequestContent(apiData, path, method))
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(c -> c.getOgcApiMediaType().type(), c -> c));
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    Optional<StylesConfiguration> stylesExtension = apiData.getExtension(StylesConfiguration.class);
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("collections")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_STYLE_METADATA_MANAGER_COLLECTION);
    String subSubPath = "/styles/{styleId}/metadata";
    String path = "/collections/{collectionId}" + subSubPath;
    List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
    Optional<OgcApiPathParameter> optCollectionIdParam =
        pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
    if (optCollectionIdParam.isEmpty()) {
      LOGGER.error(
          "Path parameter 'collectionId' missing for resource at path '"
              + path
              + "'. The resource will not be available.");
    } else {
      final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
      final boolean explode = collectionIdParam.isExplodeInOpenApi(apiData);
      final List<String> collectionIds =
          (explode) ? collectionIdParam.getValues(apiData) : ImmutableList.of("{collectionId}");
      for (String collectionId : collectionIds) {
        List<OgcApiQueryParameter> queryParameters =
            getQueryParameters(extensionRegistry, apiData, path, collectionId, HttpMethods.PUT);
        List<ApiHeader> headers =
            getHeaders(extensionRegistry, apiData, path, collectionId, HttpMethods.PUT);
        String operationSummary =
            "update the metadata document of a style in the feature collection '"
                + collectionId
                + "'";
        String description = "Update the style metadata for the style with the id `styleId`";
        if (stylesExtension.map(StylesConfiguration::isValidationEnabled).orElse(false)) {
          description +=
              " or just validate the style metadata.\n"
                  + "If the header `Prefer` is set to `handling=strict`, the style will be validated before adding "
                  + "the style to the server. If the parameter `dry-run` is set to `true`, the server will "
                  + "not be changed and only any validation errors will be reported";
        }
        description += ".\n" + "This operation replaces the complete metadata document.\n";
        // TODO document rules, e.g.wrt links
        Optional<String> operationDescription = Optional.of(description);
        String resourcePath = "/collections/" + collectionId + subSubPath;
        ImmutableOgcApiResourceData.Builder resourceBuilder =
            new ImmutableOgcApiResourceData.Builder()
                .path(resourcePath)
                .pathParameters(pathParameters);
        Map<MediaType, ApiMediaTypeContent> requestContent =
            collectionId.startsWith("{")
                ? getRequestContent(apiData, Optional.empty(), subSubPath, HttpMethods.PUT)
                : getRequestContent(
                    apiData, Optional.of(collectionId), subSubPath, HttpMethods.PUT);
        ApiOperation.of(
                resourcePath,
                HttpMethods.PUT,
                requestContent,
                queryParameters,
                headers,
                operationSummary,
                operationDescription,
                Optional.empty(),
                getOperationId("replaceStyleMetadata", collectionId),
                TAGS)
            .ifPresent(
                operation -> resourceBuilder.putOperations(HttpMethods.PUT.name(), operation));

        queryParameters =
            getQueryParameters(extensionRegistry, apiData, path, collectionId, HttpMethods.PATCH);
        headers = getHeaders(extensionRegistry, apiData, path, collectionId, HttpMethods.PATCH);
        operationSummary =
            "update parts of the style metadata document of a style in the feature collection '"
                + collectionId
                + "'";
        description =
            "Update selected elements of the style metadata for the style with the id `styleId`";
        if (stylesExtension.map(StylesConfiguration::isValidationEnabled).orElse(false)) {
          description +=
              " or just validate the style metadata.\n"
                  + "If the header `Prefer` is set to `handling=strict`, the style will be validated before adding "
                  + "the style to the server. If the parameter `dry-run` is set to `true`, the server will "
                  + "not be changed and only any validation errors will be reported";
        }
        description +=
            ".\n"
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
                + "these members, you have to send the complete new array value.";
        // TODO document rules, e.g.wrt links
        operationDescription = Optional.of(description);
        requestContent =
            collectionId.startsWith("{")
                ? getRequestContent(apiData, Optional.empty(), subSubPath, HttpMethods.PATCH)
                : getRequestContent(
                    apiData, Optional.of(collectionId), subSubPath, HttpMethods.PATCH);
        ApiOperation.of(
                resourcePath,
                HttpMethods.PATCH,
                requestContent,
                queryParameters,
                headers,
                operationSummary,
                operationDescription,
                Optional.empty(),
                getOperationId("updateStyleMetadata", collectionId),
                TAGS)
            .ifPresent(
                operation -> resourceBuilder.putOperations(HttpMethods.PATCH.name(), operation));

        definitionBuilder.putResources(resourcePath, resourceBuilder.build());
      }
    }

    return definitionBuilder.build();
  }

  /**
   * updates the metadata document of a style
   *
   * @param styleId the local identifier of a specific style
   * @return empty response (204)
   */
  @Path("/{collectionId}/styles/{styleId}/metadata")
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  public Response putStyleMetadata(
      @Auth Optional<User> optionalUser,
      @PathParam("collectionId") String collectionId,
      @PathParam("styleId") String styleId,
      @DefaultValue("false") @QueryParam("dry-run") boolean dryRun,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @Context HttpServletRequest request,
      byte[] requestBody) {

    OgcApiDataV2 apiData = api.getData();
    checkPathParameter(
        extensionRegistry,
        apiData,
        "/collections/{collectionId}/styles/{styleId}/metadata",
        "collectionId",
        collectionId);
    checkPathParameter(
        extensionRegistry,
        apiData,
        "/collections/{collectionId}/styles/{styleId}/metadata",
        "styleId",
        styleId);

    QueriesHandlerStylesManager.QueryInputStyleMetadata queryInput =
        new ImmutableQueryInputStyleMetadata.Builder()
            .collectionId(collectionId)
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
  @Path("/{collectionId}/styles/{styleId}/metadata")
  @PATCH
  @Consumes(MediaType.APPLICATION_JSON)
  public Response patchStyleMetadata(
      @Auth Optional<User> optionalUser,
      @PathParam("collectionId") String collectionId,
      @PathParam("styleId") String styleId,
      @DefaultValue("false") @QueryParam("dry-run") boolean dryRun,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @Context HttpServletRequest request,
      byte[] requestBody) {

    OgcApiDataV2 apiData = api.getData();
    checkPathParameter(
        extensionRegistry,
        apiData,
        "/collections/{collectionId}/styles/{styleId}/metadata",
        "collectionId",
        collectionId);
    checkPathParameter(
        extensionRegistry,
        apiData,
        "/collections/{collectionId}/styles/{styleId}/metadata",
        "styleId",
        styleId);

    QueriesHandlerStylesManager.QueryInputStyleMetadata queryInput =
        new ImmutableQueryInputStyleMetadata.Builder()
            .collectionId(collectionId)
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
