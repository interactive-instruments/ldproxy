/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.features.search.domain.ImmutableQueryExpression;
import de.ii.ogcapi.features.search.domain.ImmutableQueryInputStoredQueryCreateReplace;
import de.ii.ogcapi.features.search.domain.ImmutableQueryInputStoredQueryDelete;
import de.ii.ogcapi.features.search.domain.QueryExpression;
import de.ii.ogcapi.features.search.domain.SearchConfiguration;
import de.ii.ogcapi.features.search.domain.SearchQueriesHandler;
import de.ii.ogcapi.features.search.domain.StoredQueryFormat;
import de.ii.ogcapi.features.search.domain.StoredQueryRepository;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiHeader;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
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
import de.ii.xtraplatform.auth.domain.User;
import io.dropwizard.auth.Auth;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
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

/** creates/updates and deletes a stored query */
@Singleton
@AutoBind
public class EndpointStoredQueriesManager extends Endpoint implements ConformanceClass {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStoredQueriesManager.class);
  private static final List<String> TAGS = ImmutableList.of("Manage stored queries");

  private final SearchQueriesHandler queryHandler;
  private final StoredQueryRepository repository;

  @Inject
  public EndpointStoredQueriesManager(
      ExtensionRegistry extensionRegistry,
      SearchQueriesHandler queryHandler,
      StoredQueryRepository repository) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
    this.repository = repository;
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of(
        "http://www.opengis.net/spec/ogcapi-features-n/0.0/conf/manage-stored-queries");
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    Optional<SearchConfiguration> extension = apiData.getExtension(SearchConfiguration.class);

    return extension
        .filter(SearchConfiguration::isEnabled)
        .filter(SearchConfiguration::isManagerEnabled)
        .isPresent();
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return SearchConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null)
      formats =
          extensionRegistry.getExtensionsForType(StoredQueryFormat.class).stream()
              .filter(StoredQueryFormat::canSupportTransactions)
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
    Optional<SearchConfiguration> config = apiData.getExtension(SearchConfiguration.class);
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("search")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_SEARCH_MANAGER);
    String path = "/search/{queryId}";
    List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
    if (pathParameters.stream().noneMatch(param -> param.getName().equals("queryId"))) {
      LOGGER.error(
          "Path parameter 'queryId' missing for resource at path '"
              + path
              + "'. The GET method will not be available.");
    } else {
      List<OgcApiQueryParameter> queryParameters =
          getQueryParameters(extensionRegistry, apiData, path, HttpMethods.PUT);
      List<ApiHeader> headers = getHeaders(extensionRegistry, apiData, path, HttpMethods.PUT);
      String operationSummary = "create/replace a stored query";
      String description =
          "Creates a new or replaces an existing stored query with the id `queryId` ";
      if (config.map(SearchConfiguration::isValidationEnabled).orElse(false)) {
        description +=
            " or just validates the query expression.\n"
                + "If the header `Prefer` is set to `handling=strict`, the query will be validated before adding "
                + "the query to the server. If the parameter `dry-run` is set to `true`, the server will "
                + "not be changed and only any validation errors will be reported";
      }
      description +=
          ".\nThe stored query definition at `/search/{queryId}/definition` " + "is updated.";
      Optional<String> operationDescription = Optional.of(description);
      ImmutableOgcApiResourceData.Builder resourceBuilder =
          new ImmutableOgcApiResourceData.Builder().path(path).pathParameters(pathParameters);
      Map<MediaType, ApiMediaTypeContent> requestContent =
          getRequestContent(apiData, path, HttpMethods.PUT);
      ApiOperation.of(
              path,
              HttpMethods.PUT,
              requestContent,
              queryParameters,
              headers,
              operationSummary,
              operationDescription,
              Optional.empty(),
              TAGS)
          .ifPresent(operation -> resourceBuilder.putOperations(HttpMethods.PUT.name(), operation));

      queryParameters = getQueryParameters(extensionRegistry, apiData, path, HttpMethods.DELETE);
      headers = getHeaders(extensionRegistry, apiData, path, HttpMethods.DELETE);
      operationSummary = "delete a stored query";
      operationDescription = Optional.of("Delete an existing stored query with the id `queryId`.");
      requestContent = getRequestContent(apiData, path, HttpMethods.DELETE);
      ApiOperation.of(
              path,
              HttpMethods.DELETE,
              requestContent,
              queryParameters,
              headers,
              operationSummary,
              operationDescription,
              Optional.empty(),
              TAGS)
          .ifPresent(
              operation -> resourceBuilder.putOperations(HttpMethods.DELETE.name(), operation));
      definitionBuilder.putResources(path, resourceBuilder.build());
    }

    return definitionBuilder.build();
  }

  /**
   * creates or updates a stored query
   *
   * @param queryId the local identifier of the query
   * @return empty response (204)
   */
  @Path("/{queryId}")
  @PUT
  public Response putStoredQuery(
      @Auth Optional<User> optionalUser,
      @PathParam("queryId") String queryId,
      @DefaultValue("false") @QueryParam("dry-run") boolean dryRun,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @Context HttpServletRequest request,
      byte[] requestBody) {

    OgcApiDataV2 apiData = api.getData();
    checkAuthorization(apiData, optionalUser);
    checkPathParameter(extensionRegistry, apiData, "/search/{queryId}", "queryId", queryId);

    QueryExpression query;
    try {
      query =
          new ImmutableQueryExpression.Builder()
              .from(QueryExpression.of(requestBody))
              .id(queryId)
              .offset(Optional.empty())
              .build();
    } catch (IOException e) {
      throw new IllegalArgumentException(
          String.format("The content of the query expression is invalid: %s", e.getMessage()));
    }

    // TODO recompute API definition of EndpointStoredQuery

    SearchQueriesHandler.QueryInputStoredQueryCreateReplace queryInput =
        new ImmutableQueryInputStoredQueryCreateReplace.Builder()
            .queryId(queryId)
            .query(query)
            .strict(strictHandling(request.getHeaders("Prefer")))
            .dryRun(dryRun)
            .build();

    return queryHandler.handle(
        SearchQueriesHandler.Query.CREATE_REPLACE, queryInput, requestContext);
  }

  /**
   * deletes a stored query
   *
   * @param queryId the local identifier of the stored query
   * @return empty response (204)
   */
  @Path("/{queryId}")
  @DELETE
  public Response deleteStoredQuery(
      @Auth Optional<User> optionalUser,
      @PathParam("queryId") String queryId,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext) {

    OgcApiDataV2 apiData = api.getData();
    checkAuthorization(apiData, optionalUser);
    checkPathParameter(extensionRegistry, apiData, "/search/{queryId}", "queryId", queryId);

    // TODO recompute API definition of EndpointStoredQuery

    SearchQueriesHandler.QueryInputStoredQueryDelete queryInput =
        new ImmutableQueryInputStoredQueryDelete.Builder().queryId(queryId).build();

    return queryHandler.handle(SearchQueriesHandler.Query.DELETE, queryInput, requestContext);
  }
}
