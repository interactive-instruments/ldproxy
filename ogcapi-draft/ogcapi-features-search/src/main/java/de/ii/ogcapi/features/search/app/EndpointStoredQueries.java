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
import de.ii.ogcapi.features.search.domain.ImmutableQueryInputStoredQueries;
import de.ii.ogcapi.features.search.domain.SearchConfiguration;
import de.ii.ogcapi.features.search.domain.SearchQueriesHandler;
import de.ii.ogcapi.features.search.domain.StoredQueriesFormat;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceSet;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @langEn This operation fetches the stored queries available on the server. For each stored query,
 *     the id, a title, links to the query ... TODO is provided.
 * @langDe TODO
 * @name Stored Queries
 * @path /{apiId}/search
 * @format {@link de.ii.ogcapi.features.search.domain.StoredQueriesFormat}
 */
@Singleton
@AutoBind
public class EndpointStoredQueries extends Endpoint implements ConformanceClass {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStoredQueries.class);

  private static final List<String> TAGS = ImmutableList.of("Discover and execute queries");

  private final SearchQueriesHandler queryHandler;

  @Inject
  public EndpointStoredQueries(
      ExtensionRegistry extensionRegistry, SearchQueriesHandler queryHandler) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of("http://www.opengis.net/spec/ogcapi-features-n/0.0/conf/core");
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return SearchConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(StoredQueriesFormat.class);
    return formats;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("search")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_SEARCH_STORED_QUERIES);
    List<OgcApiQueryParameter> queryParameters =
        getQueryParameters(extensionRegistry, apiData, "/search");
    String operationSummary = "lists the available stored queries";
    Optional<String> operationDescription =
        Optional.of(
            "This operation fetches the set of stored queries available in this API. "
                + "For each stored query the id, a title, links to the TODO is provided.");
    String path = "/search";
    ImmutableOgcApiResourceSet.Builder resourceBuilderSet =
        new ImmutableOgcApiResourceSet.Builder().path(path).subResourceType("QueryExpression");
    ApiOperation.getResource(
            apiData,
            path,
            false,
            queryParameters,
            ImmutableList.of(),
            getContent(apiData, path),
            operationSummary,
            operationDescription,
            Optional.empty(),
            TAGS)
        .ifPresent(operation -> resourceBuilderSet.putOperations("GET", operation));
    definitionBuilder.putResources(path, resourceBuilderSet.build());

    return definitionBuilder.build();
  }

  /**
   * fetch all available stored queries
   *
   * @return all stored queries
   */
  @GET
  public Response getStoredQueries(@Context OgcApi api, @Context ApiRequestContext requestContext) {
    SearchQueriesHandler.QueryInputStoredQueries queryInput =
        new ImmutableQueryInputStoredQueries.Builder()
            .from(getGenericQueryInput(api.getData()))
            .build();

    return queryHandler.handle(
        SearchQueriesHandler.Query.STORED_QUERIES, queryInput, requestContext);
  }
}
