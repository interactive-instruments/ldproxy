/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.infra;

import static de.ii.ogcapi.styles.domain.QueriesHandlerStyles.SCOPE_STYLES_WRITE;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
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
import de.ii.ogcapi.resources.app.ResourcesBuildingBlock;
import de.ii.ogcapi.resources.domain.ResourceFormatExtension;
import de.ii.ogcapi.resources.domain.ResourcesConfiguration;
import de.ii.ogcapi.styles.domain.StylesConfiguration;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.store.domain.BlobStore;
import io.dropwizard.auth.Auth;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @title Resource
 * @path resources/{resourceId}
 * @langEn Create, update or delete a file resource.
 * @langDe Erzeugen, Aktualisieren oder Löschen einer Dateiressource.
 * @ref:formats {@link de.ii.ogcapi.resources.domain.ResourceFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointResourcesManager extends Endpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointResourcesManager.class);
  private static final List<String> TAGS =
      ImmutableList.of("Create, update and delete other resources");

  private final BlobStore resourcesStore;

  @Inject
  public EndpointResourcesManager(BlobStore blobStore, ExtensionRegistry extensionRegistry) {
    super(extensionRegistry);

    this.resourcesStore = blobStore.with(ResourcesBuildingBlock.STORE_RESOURCE_TYPE);
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
            .getExtension(ResourcesConfiguration.class)
            .map(ResourcesConfiguration::isManagerEnabled)
            .orElse(false)
        || apiData
            .getExtension(StylesConfiguration.class)
            .map(StylesConfiguration::isResourceManagerEnabled)
            .orElse(false);
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return ResourcesConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getResourceFormats() {
    if (formats == null)
      formats =
          extensionRegistry.getExtensionsForType(ResourceFormatExtension.class).stream()
              .filter(ResourceFormatExtension::canSupportTransactions)
              .collect(Collectors.toList());
    return formats;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("resources")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_RESOURCES_MANAGER);
    String path = "/resources/{resourceId}";
    HttpMethods methodReplace = HttpMethods.PUT;
    List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
    List<OgcApiQueryParameter> queryParameters =
        getQueryParameters(extensionRegistry, apiData, path, methodReplace);
    String operationSummary = "replace a file resource or add a new one";
    Optional<String> operationDescription =
        Optional.of(
            "Replace an existing resource with the id `resourceId`. If no "
                + "such resource exists, a new resource with that id is added. "
                + "A sprite used in a Mapbox Style stylesheet consists of "
                + "three resources. Each of the resources needs to be created "
                + "(and eventually deleted) separately.\n"
                + "The PNG bitmap image (resourceId ends in '.png'), the JSON "
                + "index file (resourceId of the same name, but ends in '.json' "
                + "instead of '.png') and the PNG  bitmap image for "
                + "high-resolution displays (the file ends in '.@2x.png').\n"
                + "The resource will only by available in the native format in "
                + "which the resource is posted. There is no support for "
                + "automated conversions to other representations.");
    ImmutableOgcApiResourceData.Builder resourceBuilder =
        new ImmutableOgcApiResourceData.Builder().path(path).pathParameters(pathParameters);
    Map<MediaType, ApiMediaTypeContent> requestContent = getRequestContent(apiData);
    ApiOperation.of(
            path,
            methodReplace,
            requestContent,
            queryParameters,
            ImmutableList.of(),
            operationSummary,
            operationDescription,
            Optional.empty(),
            getOperationId("createOrReplaceResource"),
            SCOPE_STYLES_WRITE,
            TAGS)
        .ifPresent(operation -> resourceBuilder.putOperations(methodReplace.name(), operation));
    HttpMethods methodDelete = HttpMethods.DELETE;
    queryParameters = getQueryParameters(extensionRegistry, apiData, path, methodDelete);
    operationSummary = "delete a file resource";
    operationDescription =
        Optional.of(
            "Delete an existing resource with the id `resourceId`. If no "
                + "such resource exists, an error is returned.");
    ApiOperation.of(
            path,
            methodDelete,
            ImmutableMap.of(),
            queryParameters,
            ImmutableList.of(),
            operationSummary,
            operationDescription,
            Optional.empty(),
            getOperationId("deleteResource"),
            SCOPE_STYLES_WRITE,
            TAGS)
        .ifPresent(operation -> resourceBuilder.putOperations(methodDelete.name(), operation));
    definitionBuilder.putResources(path, resourceBuilder.build());

    return definitionBuilder.build();
  }

  /**
   * create or update a resource
   *
   * @param resourceId the local identifier of a specific style
   * @return empty response (204)
   */
  @Path("/{resourceId}")
  @PUT
  @Consumes(MediaType.WILDCARD)
  public Response putResource(
      @Auth Optional<User> optionalUser,
      @PathParam("resourceId") String resourceId,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @Context HttpServletRequest request,
      byte[] requestBody)
      throws IOException {

    return getResourceFormats().stream()
        .filter(format -> requestContext.getMediaType().matches(format.getMediaType().type()))
        .findAny()
        .map(ResourceFormatExtension.class::cast)
        .orElseThrow(
            () ->
                new NotSupportedException(
                    MessageFormat.format(
                        "The provided media type ''{0}'' is not supported for this resource.",
                        requestContext.getMediaType())))
        .putResource(requestBody, resourceId, api.getData(), requestContext);
  }

  /**
   * deletes a resource
   *
   * @param resourceId the local identifier of a specific style
   * @return empty response (204)
   */
  @Path("/{resourceId}")
  @DELETE
  public Response deleteResource(
      @Auth Optional<User> optionalUser,
      @PathParam("resourceId") String resourceId,
      @Context OgcApi dataset) {

    try {
      resourcesStore.delete(java.nio.file.Path.of(dataset.getId(), resourceId));
    } catch (IOException e) {
      // ignore
    }

    return Response.noContent().build();
  }
}
