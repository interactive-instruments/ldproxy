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
import de.ii.ogcapi.features.search.domain.ImmutableQueryInputQueryDefinition;
import de.ii.ogcapi.features.search.domain.QueryExpression;
import de.ii.ogcapi.features.search.domain.SearchConfiguration;
import de.ii.ogcapi.features.search.domain.SearchQueriesHandler;
import de.ii.ogcapi.features.search.domain.SearchQueriesHandler.Query;
import de.ii.ogcapi.features.search.domain.SearchQueriesHandler.QueryInputQueryDefinition;
import de.ii.ogcapi.features.search.domain.StoredQueryFormat;
import de.ii.ogcapi.features.search.domain.StoredQueryRepository;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiHeader;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** creates/updates and deletes a stored query */
@Singleton
@AutoBind
public class EndpointStoredQueryDefinition extends Endpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStoredQueryDefinition.class);
  private static final List<String> TAGS = ImmutableList.of("Manage stored queries");

  private final SearchQueriesHandler queryHandler;
  private final StoredQueryRepository repository;

  @Inject
  public EndpointStoredQueryDefinition(
      ExtensionRegistry extensionRegistry,
      SearchQueriesHandler queryHandler,
      StoredQueryRepository repository) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
    this.repository = repository;
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

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    Optional<SearchConfiguration> config = apiData.getExtension(SearchConfiguration.class);
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("search")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_SEARCH_MANAGER);
    String path = "/search/{queryId}/definition";
    List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
    if (pathParameters.stream().noneMatch(param -> param.getName().equals("queryId"))) {
      LOGGER.error(
          "Path parameter 'queryId' missing for resource at path '"
              + path
              + "'. The GET method will not be available.");
    } else {
      List<OgcApiQueryParameter> queryParameters =
          getQueryParameters(extensionRegistry, apiData, path, HttpMethods.GET);
      List<ApiHeader> headers = getHeaders(extensionRegistry, apiData, path, HttpMethods.GET);
      String operationSummary = "fetch the definition of a stored query";
      String description = "Retrieves the definition of the stored query with the id `queryId`.";
      Optional<String> operationDescription = Optional.of(description);
      ImmutableOgcApiResourceData.Builder resourceBuilder =
          new ImmutableOgcApiResourceData.Builder().path(path).pathParameters(pathParameters);
      ApiOperation.getResource(
              apiData,
              path,
              false,
              queryParameters,
              headers,
              getContent(apiData, path),
              operationSummary,
              operationDescription,
              Optional.empty(),
              getOperationId("getStoredQueryDefinition"),
              TAGS)
          .ifPresent(operation -> resourceBuilder.putOperations(HttpMethods.GET.name(), operation));
      definitionBuilder.putResources(path, resourceBuilder.build());
    }

    return definitionBuilder.build();
  }

  /**
   * Fetch a query expression by id
   *
   * @param queryId the local identifier of the query
   * @return the query expression
   */
  @Path("/{queryId}/definition")
  @GET
  public Response getQueryExpression(
      @PathParam("queryId") String queryId,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext) {

    OgcApiDataV2 apiData = api.getData();
    checkPathParameter(
        extensionRegistry, apiData, "/search/{queryId}/definition", "queryId", queryId);

    QueryExpression query = repository.get(apiData, queryId);

    QueryInputQueryDefinition queryInput =
        new ImmutableQueryInputQueryDefinition.Builder()
            .from(getGenericQueryInput(api.getData()))
            .queryId(queryId)
            .query(query)
            .lastModified(repository.getLastModified(apiData, queryId))
            .build();

    return queryHandler.handle(Query.DEFINITION, queryInput, requestContext);
  }
}
