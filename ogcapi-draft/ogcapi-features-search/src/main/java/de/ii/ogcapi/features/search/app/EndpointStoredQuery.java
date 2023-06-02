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
import com.google.common.collect.ImmutableList.Builder;
import de.ii.ogcapi.features.core.domain.EndpointRequiresFeatures;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.search.domain.ImmutableQueryExpression;
import de.ii.ogcapi.features.search.domain.ImmutableQueryInputQuery;
import de.ii.ogcapi.features.search.domain.QueryExpression;
import de.ii.ogcapi.features.search.domain.SearchConfiguration;
import de.ii.ogcapi.features.search.domain.SearchQueriesHandler;
import de.ii.ogcapi.features.search.domain.SearchQueriesHandler.Query;
import de.ii.ogcapi.features.search.domain.SearchQueriesHandler.QueryInputQuery;
import de.ii.ogcapi.features.search.domain.StoredQueryRepository;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.QueryParameterSet;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.util.List;
import java.util.Objects;
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
 * @title Stored Query
 * @path search/{queryId}
 * @langEn Execute the stored query. Parameters are submitted as query parameters.
 * @langDe Führt die gespeicherte Abfrage aus. Parameter werden als Abfrageparameter übergeben.
 * @ref:formats {@link de.ii.ogcapi.features.core.domain.FeatureFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointStoredQuery extends EndpointRequiresFeatures {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStoredQuery.class);

  private static final List<String> TAGS = ImmutableList.of("Discover and execute queries");

  private final FeaturesCoreProviders providers;
  private final StoredQueryRepository repository;
  private final SearchQueriesHandler queryHandler;
  private final SchemaValidator schemaValidator;

  @Inject
  public EndpointStoredQuery(
      ExtensionRegistry extensionRegistry,
      FeaturesCoreProviders providers,
      StoredQueryRepository repository,
      SearchQueriesHandler queryHandler,
      SchemaValidator schemaValidator) {
    super(extensionRegistry);
    this.providers = providers;
    this.repository = repository;
    this.queryHandler = queryHandler;
    this.schemaValidator = schemaValidator;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return SearchConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getResourceFormats() {
    if (formats == null) {
      formats = extensionRegistry.getExtensionsForType(FeatureFormatExtension.class);
    }
    return formats;
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    ValidationResult result = super.onStartup(api, apiValidation);

    if (apiValidation == MODE.NONE) {
      return result;
    }

    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().from(result).mode(apiValidation);

    builder = repository.validate(builder, api.getData());

    return builder.build();
  }

  // TODO temporary fix, Endpoint.getDefinition() for now is no longer final;
  //      update with https://github.com/interactive-instruments/ldproxy/issues/843
  @Override
  public ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
    if (!isEnabledForApi(apiData)) {
      return super.getDefinition(apiData);
    }

    return apiDefinitions.computeIfAbsent(
        // override to trigger update when stored queries have changed
        repository.getAll(apiData).hashCode(),
        ignore -> {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Generating API definition for {}", this.getClass().getSimpleName());
          }

          ApiEndpointDefinition apiEndpointDefinition = computeDefinition(apiData);

          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "Finished generating API definition for {}", this.getClass().getSimpleName());
          }

          return apiEndpointDefinition;
        });
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("search")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_SEARCH_STORED_QUERY);

    repository
        .getAll(apiData)
        .forEach(
            query -> {
              String queryId = query.getId();
              String path = "/search/" + queryId;
              String definitionPath = "/search/{queryId}";
              Builder<OgcApiQueryParameter> paramsBuilder = ImmutableList.builder();
              paramsBuilder.addAll(getQueryParameters(extensionRegistry, apiData, definitionPath));

              String operationSummary = "execute stored query " + query.getTitle().orElse(queryId);
              Optional<String> operationDescription = query.getDescription();
              ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder =
                  new ImmutableOgcApiResourceAuxiliary.Builder().path(path);
              ApiOperation.getResource(
                      apiData,
                      path,
                      false,
                      paramsBuilder.build(),
                      ImmutableList.of(),
                      getResponseContent(apiData),
                      operationSummary,
                      operationDescription,
                      Optional.empty(),
                      getOperationId("executeStoredQuery"),
                      TAGS)
                  .ifPresent(operation -> resourceBuilder.putOperations("GET", operation));
              definitionBuilder.putResources(path, resourceBuilder.build());
            });

    return definitionBuilder.build();
  }

  /**
   * Execute a query by id
   *
   * @param queryId the local identifier of the query
   * @return the query result
   */
  @Path("/{queryId}")
  @GET
  public Response getStoredQuery(
      @PathParam("queryId") String queryId,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext) {

    OgcApiDataV2 apiData = api.getData();
    ensureSupportForFeatures(apiData);
    checkPathParameter(extensionRegistry, apiData, "/search/{queryId}", "queryId", queryId);

    QueryExpression query = repository.get(apiData, queryId);

    List<OgcApiQueryParameter> parameterDefinitions =
        getQueryParameters(extensionRegistry, apiData, "/search/{queryId}");
    QueryParameterSet queryParameterSet =
        QueryParameterSet.of(parameterDefinitions, requestContext.getParameters())
            .evaluate(api, Optional.empty());

    // TODO #846
    final String offset = requestContext.getParameters().get("offset");
    if (Objects.nonNull(offset)) {
      query =
          new ImmutableQueryExpression.Builder()
              .from(query)
              .offset(Integer.parseInt(offset))
              .build();
    }
    query = query.resolveParameters(requestContext.getParameters(), schemaValidator);

    FeaturesCoreConfiguration coreConfiguration =
        apiData.getExtension(FeaturesCoreConfiguration.class).orElseThrow();

    QueryInputQuery queryInput =
        new ImmutableQueryInputQuery.Builder()
            .from(getGenericQueryInput(apiData))
            .query(query)
            .featureProvider(providers.getFeatureProviderOrThrow(apiData))
            .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
            .minimumPageSize(Optional.ofNullable(coreConfiguration.getMinimumPageSize()))
            .defaultPageSize(Optional.ofNullable(coreConfiguration.getDefaultPageSize()))
            .maximumPageSize(Optional.ofNullable(coreConfiguration.getMaximumPageSize()))
            .showsFeatureSelfLink(
                Objects.equals(coreConfiguration.getShowsFeatureSelfLink(), Boolean.TRUE))
            .allLinksAreLocal(
                api.getData()
                    .getExtension(SearchConfiguration.class)
                    .map(SearchConfiguration::getAllLinksAreLocal)
                    .orElse(false))
            .profileIsApplicable(
                apiData.getCollections().values().stream()
                    .anyMatch(
                        collectionData ->
                            providers
                                .getFeatureSchema(apiData, collectionData)
                                .map(
                                    schema ->
                                        schema.getAllNestedProperties().stream()
                                            .anyMatch(SchemaBase::isFeatureRef))
                                .orElse(false)))
            .isStoredQuery(true)
            .build();

    return queryHandler.handle(Query.QUERY, queryInput, requestContext);
  }
}
