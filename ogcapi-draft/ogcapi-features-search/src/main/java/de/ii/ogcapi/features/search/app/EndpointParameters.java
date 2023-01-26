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
import de.ii.ogcapi.features.search.domain.ImmutableQueryInputParameters;
import de.ii.ogcapi.features.search.domain.ParametersFormat;
import de.ii.ogcapi.features.search.domain.QueryExpression;
import de.ii.ogcapi.features.search.domain.SearchConfiguration;
import de.ii.ogcapi.features.search.domain.SearchQueriesHandler;
import de.ii.ogcapi.features.search.domain.SearchQueriesHandler.Query;
import de.ii.ogcapi.features.search.domain.SearchQueriesHandler.QueryInputParameters;
import de.ii.ogcapi.features.search.domain.StoredQueryRepository;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @title Stored Query Parameters
 * @path search/{queryId}/parameters
 * @langEn Get the definition of the query parameters
 * @langDe Abrufen der Definition der Abfrageparameter
 * @ref:formats {@link de.ii.ogcapi.features.search.domain.ParametersFormat}
 */
@Singleton
@AutoBind
public class EndpointParameters extends Endpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointParameters.class);

  private static final List<String> TAGS = ImmutableList.of("Discover and execute queries");

  private final StoredQueryRepository repository;
  private final SearchQueriesHandler queryHandler;
  private final I18n i18n;

  @Inject
  public EndpointParameters(
      ExtensionRegistry extensionRegistry,
      StoredQueryRepository repository,
      I18n i18n,
      SearchQueriesHandler queryHandler) {
    super(extensionRegistry);
    this.repository = repository;
    this.i18n = i18n;
    this.queryHandler = queryHandler;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return SearchConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getResourceFormats() {
    if (formats == null) formats = extensionRegistry.getExtensionsForType(ParametersFormat.class);
    return formats;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("search")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_SEARCH_PARAMETERS);

    String path = "/search/{queryId}/parameters";
    List<OgcApiQueryParameter> params = getQueryParameters(extensionRegistry, apiData, path);
    List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
    if (pathParameters.stream().noneMatch(param -> param.getName().equals("queryId"))) {
      LOGGER.error(
          "Path parameter 'queryId' missing for resource at path '"
              + path
              + "'. The GET method will not be available.");
    } else {
      String operationSummary = "fetch parameters of stored query {queryId}";
      ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder =
          new ImmutableOgcApiResourceAuxiliary.Builder().path(path).pathParameters(pathParameters);
      ApiOperation.getResource(
              apiData,
              path,
              false,
              params,
              ImmutableList.of(),
              getResponseContent(apiData),
              operationSummary,
              Optional.empty(),
              Optional.empty(),
              getOperationId("getStoredQueryParameters"),
              TAGS)
          .ifPresent(operation -> resourceBuilder.putOperations("GET", operation));
      definitionBuilder.putResources(path, resourceBuilder.build());
    }

    return definitionBuilder.build();
  }

  /**
   * Fetch the parameters of a stored query
   *
   * @param queryId the local identifier of the query
   * @return the query result
   */
  @Path("/{queryId}/parameters")
  @GET
  public Response getParameters(
      @PathParam("queryId") String queryId,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext) {

    OgcApiDataV2 apiData = api.getData();
    checkPathParameter(
        extensionRegistry, apiData, "/search/{queryId}/parameters", "queryId", queryId);

    QueryExpression query = repository.get(apiData, queryId);

    QueryInputParameters queryInput =
        new ImmutableQueryInputParameters.Builder()
            .from(getGenericQueryInput(api.getData()))
            .queryId(queryId)
            .query(query)
            .build();

    return queryHandler.handle(Query.PARAMETERS, queryInput, requestContext);
  }
}
