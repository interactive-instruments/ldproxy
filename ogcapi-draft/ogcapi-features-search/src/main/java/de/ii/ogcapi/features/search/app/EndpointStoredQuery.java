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
import de.ii.ogcapi.features.search.domain.StoredQueryFormat;
import de.ii.ogcapi.features.search.domain.StoredQueryRepository;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
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
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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
public class EndpointStoredQuery extends Endpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointStoredQuery.class);

  private static final List<String> TAGS = ImmutableList.of("Discover and execute queries");

  private final FeaturesCoreProviders providers;
  private final StoredQueryRepository repository;
  private final SearchQueriesHandler queryHandler;
  private final SchemaValidator schemaValidator;
  private final I18n i18n;

  @Inject
  public EndpointStoredQuery(
      ExtensionRegistry extensionRegistry,
      FeaturesCoreProviders providers,
      StoredQueryRepository repository,
      I18n i18n,
      SearchQueriesHandler queryHandler,
      SchemaValidator schemaValidator) {
    super(extensionRegistry);
    this.providers = providers;
    this.repository = repository;
    this.i18n = i18n;
    this.queryHandler = queryHandler;
    this.schemaValidator = schemaValidator;
  }

  private Stream<StoredQueryFormat> getStoredQueryFormatStream(OgcApiDataV2 apiData) {
    return extensionRegistry.getExtensionsForType(StoredQueryFormat.class).stream()
        .filter(format -> format.isEnabledForApi(apiData));
  }

  private List<ApiMediaType> getStoredQueryMediaTypes(
      OgcApiDataV2 apiData, File apiDir, String queryId) {
    return getStoredQueryFormatStream(apiData)
        .filter(
            format ->
                new File(apiDir + File.separator + queryId + "." + format.getFileExtension())
                    .exists())
        .map(StoredQueryFormat::getMediaType)
        .collect(Collectors.toList());
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return SearchConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(FeatureFormatExtension.class);
    return formats;
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    ValidationResult result = super.onStartup(api, apiValidation);

    if (apiValidation == MODE.NONE) return result;

    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().from(result).mode(apiValidation);

    builder = repository.validate(builder, api.getData());

    return builder.build();
  }

  // TODO temporary fix
  @Override
  public ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
    if (!isEnabledForApi(apiData)) {
      return super.getDefinition(apiData);
    }

    return apiDefinitions.computeIfAbsent(
        // TODO temporary override to trigger update when stored queries have changed
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
              String queryId =
                  query
                      .getId()
                      .orElseThrow(
                          () -> new IllegalStateException("Found stored query without an id."));
              String path = "/search/" + queryId;
              String definitionPath = "/search/{queryId}";
              Builder<OgcApiQueryParameter> paramsBuilder = ImmutableList.builder();
              paramsBuilder.addAll(getQueryParameters(extensionRegistry, apiData, definitionPath));

              query
                  .getParametersWithOpenApiSchema()
                  .forEach(
                      (name, schema) -> {
                        String description = name;
                        if (Objects.nonNull(schema.getTitle()) && !schema.getTitle().isEmpty()) {
                          description = schema.getTitle();
                          if (Objects.nonNull(schema.getDescription())
                              && !schema.getDescription().isEmpty()) {
                            description += ": " + schema.getDescription();
                          } else {
                            description += ".";
                          }
                        }
                        paramsBuilder.add(
                            ImmutableQueryParameterTemplateParameter.builder()
                                .apiId(apiData.getId())
                                .queryId(queryId)
                                .name(name)
                                .description(description)
                                .schema(schema)
                                .schemaValidator(schemaValidator)
                                .build());
                      });

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
                      getContent(apiData, path),
                      operationSummary,
                      operationDescription,
                      Optional.empty(),
                      getOperationId("executeStoredQuery"),
                      TAGS)
                  .ifPresent(operation -> resourceBuilder.putOperations("GET", operation));
              definitionBuilder.putResources(path, resourceBuilder.build());
            });

    // TODO add POST

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
    checkPathParameter(extensionRegistry, apiData, "/search/{queryId}", "queryId", queryId);

    QueryExpression query = repository.get(apiData, queryId);

    final String offset = requestContext.getParameters().get("offset");
    if (Objects.nonNull(offset)) {
      query =
          new ImmutableQueryExpression.Builder()
              .from(query)
              .offset(Integer.parseInt(offset))
              .build();
    }

    // TODO centralize in superclass or helper
    FeaturesCoreConfiguration coreConfiguration =
        api.getData()
            .getExtension(FeaturesCoreConfiguration.class)
            .filter(ExtensionConfiguration::isEnabled)
            .filter(
                cfg ->
                    cfg.getItemType().orElse(FeaturesCoreConfiguration.ItemType.feature)
                        != FeaturesCoreConfiguration.ItemType.unknown)
            .orElseThrow(() -> new NotFoundException("Features are not supported for this API."));

    QueryInputQuery queryInput =
        new ImmutableQueryInputQuery.Builder()
            .from(getGenericQueryInput(api.getData()))
            .query(query)
            .featureProvider(providers.getFeatureProviderOrThrow(api.getData()))
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
            .build();

    return queryHandler.handle(Query.QUERY, queryInput, requestContext);
  }
}
