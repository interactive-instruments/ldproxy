/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.ImmutableFeatureTransformationContextGeneric;
import de.ii.ogcapi.features.search.domain.ImmutableParameter;
import de.ii.ogcapi.features.search.domain.ImmutableParameters;
import de.ii.ogcapi.features.search.domain.ImmutableStoredQueries;
import de.ii.ogcapi.features.search.domain.ImmutableStoredQuery;
import de.ii.ogcapi.features.search.domain.Parameter;
import de.ii.ogcapi.features.search.domain.ParameterFormat;
import de.ii.ogcapi.features.search.domain.Parameters;
import de.ii.ogcapi.features.search.domain.ParametersFormat;
import de.ii.ogcapi.features.search.domain.QueryExpression;
import de.ii.ogcapi.features.search.domain.QueryExpression.FilterOperator;
import de.ii.ogcapi.features.search.domain.SearchConfiguration;
import de.ii.ogcapi.features.search.domain.SearchQueriesHandler;
import de.ii.ogcapi.features.search.domain.StoredQueries;
import de.ii.ogcapi.features.search.domain.StoredQueriesFormat;
import de.ii.ogcapi.features.search.domain.StoredQueryFormat;
import de.ii.ogcapi.features.search.domain.StoredQueryRepository;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.HeaderCaching;
import de.ii.ogcapi.foundation.domain.HeaderContentDisposition;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueryHandler;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.cql.domain.And;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.Cql.Format;
import de.ii.xtraplatform.cql.domain.Cql2Expression;
import de.ii.xtraplatform.cql.domain.CqlParseException;
import de.ii.xtraplatform.cql.domain.Or;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureStream;
import de.ii.xtraplatform.features.domain.FeatureStream.Result;
import de.ii.xtraplatform.features.domain.FeatureStream.ResultBase;
import de.ii.xtraplatform.features.domain.FeatureStream.ResultReduced;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.ImmutableMultiFeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableMultiFeatureQuery.Builder;
import de.ii.xtraplatform.features.domain.ImmutableSubQuery;
import de.ii.xtraplatform.features.domain.MultiFeatureQuery;
import de.ii.xtraplatform.features.domain.MultiFeatureQuery.SubQuery;
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SortKey;
import de.ii.xtraplatform.features.domain.SortKey.Direction;
import de.ii.xtraplatform.features.domain.TypeQuery;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import de.ii.xtraplatform.streams.domain.OutputStreamToByteConsumer;
import de.ii.xtraplatform.streams.domain.Reactive.Sink;
import de.ii.xtraplatform.streams.domain.Reactive.SinkReduced;
import de.ii.xtraplatform.streams.domain.Reactive.SinkTransformed;
import de.ii.xtraplatform.web.domain.ETag;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.Unit;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.kortforsyningen.proj.Units;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class SearchQueriesHandlerImpl implements SearchQueriesHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SearchQueriesHandlerImpl.class);

  class IdTransform implements PropertyTransformations {

    private final Map<String, List<PropertyTransformation>> transformations;

    IdTransform(FeatureProvider2 featureProvider, String featureTypeId, String collectionId) {
      FeatureSchema schema =
          Objects.requireNonNull(featureProvider.getData().getTypes().get(featureTypeId));
      String idProperty = Objects.requireNonNull(findIdProperty(schema));
      transformations =
          ImmutableMap.of(
              idProperty,
              ImmutableList.of(
                  new ImmutablePropertyTransformation.Builder()
                      .stringFormat(String.format("%s.{{value}}", collectionId))
                      .build()));
    }

    private String findIdProperty(FeatureSchema schema) {
      return schema.getProperties().stream()
          .flatMap(
              property -> {
                Collection<FeatureSchema> nestedProperties = property.getAllNestedProperties();
                if (!nestedProperties.isEmpty()) {
                  return nestedProperties.stream();
                }
                return Stream.of(property);
              })
          .filter(property -> property.getRole().isPresent() && property.getRole().get() == Role.ID)
          .findFirst()
          .map(FeatureSchema::getFullPathAsString)
          .orElse(null);
    }

    @Override
    public Map<String, List<PropertyTransformation>> getTransformations() {
      return transformations;
    }
  }

  private final I18n i18n;
  private final CrsTransformerFactory crsTransformerFactory;
  private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;
  private final ExtensionRegistry extensionRegistry;
  private final EntityRegistry entityRegistry;
  private final CrsInfo crsInfo;
  private final Cql cql;
  private final StoredQueryRepository repository;
  private final StoredQueriesLinkGenerator linkGenerator;
  private final SchemaValidator schemaValidator;

  @Inject
  public SearchQueriesHandlerImpl(
      I18n i18n,
      CrsTransformerFactory crsTransformerFactory,
      ExtensionRegistry extensionRegistry,
      EntityRegistry entityRegistry,
      CrsInfo crsInfo,
      Cql cql,
      StoredQueryRepository repository,
      SchemaValidator schemaValidator) {
    this.i18n = i18n;
    this.crsTransformerFactory = crsTransformerFactory;
    this.extensionRegistry = extensionRegistry;
    this.entityRegistry = entityRegistry;
    this.crsInfo = crsInfo;
    this.cql = cql;
    this.repository = repository;
    this.schemaValidator = schemaValidator;
    this.linkGenerator = new StoredQueriesLinkGenerator();

    this.queryHandlers =
        ImmutableMap.of(
            Query.STORED_QUERIES,
                QueryHandler.with(QueryInputStoredQueries.class, this::getStoredQueries),
            Query.QUERY, QueryHandler.with(QueryInputQuery.class, this::executeQuery),
            Query.DEFINITION,
                QueryHandler.with(QueryInputQueryDefinition.class, this::getQueryDefinition),
            Query.PARAMETERS, QueryHandler.with(QueryInputParameters.class, this::getParameters),
            Query.PARAMETER, QueryHandler.with(QueryInputParameter.class, this::getParameter),
            Query.CREATE_REPLACE,
                QueryHandler.with(QueryInputStoredQueryCreateReplace.class, this::writeStoredQuery),
            Query.DELETE,
                QueryHandler.with(QueryInputStoredQueryDelete.class, this::deleteStoredQuery));
  }

  @Override
  public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
    return queryHandlers;
  }

  public static void ensureCollectionIdExists(OgcApiDataV2 apiData, String collectionId) {
    if (!apiData.isCollectionEnabled(collectionId)) {
      throw new NotFoundException(
          MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));
    }
  }

  private static void ensureFeatureProviderSupportsQueries(FeatureProvider2 featureProvider) {
    if (!featureProvider.supportsQueries()) {
      throw new IllegalStateException("Feature provider does not support queries.");
    }
  }

  private Response writeStoredQuery(
      QueryInputStoredQueryCreateReplace queryInput, ApiRequestContext requestContext) {
    if (queryInput.getStrict()) {
      // TODO which additional checks? parsing all filters?
    }

    if (!queryInput.getDryRun()) {
      try {
        repository.writeStoredQueryDocument(
            requestContext.getApi().getData(), queryInput.getQueryId(), queryInput.getQuery());
      } catch (IOException e) {
        throw new IllegalStateException(
            MessageFormat.format("Error while storing query '{0}'.", queryInput.getQueryId()), e);
      }
    }

    return Response.noContent().build();
  }

  private Response deleteStoredQuery(
      QueryInputStoredQueryDelete queryInput, ApiRequestContext requestContext) {
    try {
      repository.deleteStoredQuery(requestContext.getApi().getData(), queryInput.getQueryId());
    } catch (IOException e) {
      throw new IllegalStateException(
          MessageFormat.format("Error while deleting stored query '{0}'.", queryInput.getQueryId()),
          e);
    }
    return Response.noContent().build();
  }

  private Response getParameters(
      QueryInputParameters queryInput, ApiRequestContext requestContext) {
    ImmutableParameters.Builder builder = new ImmutableParameters.Builder();

    OgcApiDataV2 apiData = requestContext.getApi().getData();

    queryInput
        .getQuery()
        .getParametersAsNodes()
        .forEach(
            (name, schema) -> {
              builder.putProperties(name, schema);
              if (schema.isObject() && !schema.has("default")) {
                builder.addRequired(name);
              }
            });

    List<Link> links =
        linkGenerator.generateLinks(
            requestContext.getUriCustomizer(),
            requestContext.getMediaType(),
            requestContext.getAlternateMediaTypes(),
            i18n,
            requestContext.getLanguage());

    builder.lastModified(Optional.ofNullable(repository.getLastModified(apiData)));

    Parameters parameters = builder.build();

    ParametersFormat format =
        extensionRegistry.getExtensionsForType(ParametersFormat.class).stream()
            .filter(f -> requestContext.getMediaType().matches(f.getMediaType().type()))
            .findAny()
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported, the following media types are available: {1}",
                            requestContext.getMediaType(),
                            extensionRegistry.getExtensionsForType(ParametersFormat.class).stream()
                                .map(f -> f.getMediaType().type().toString())
                                .collect(Collectors.joining(", ")))));

    Date lastModified = getLastModified(queryInput, parameters);
    EntityTag etag =
        !format.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
                || apiData
                    .getExtension(HtmlConfiguration.class)
                    .map(HtmlConfiguration::getSendEtags)
                    .orElse(false)
            ? ETag.from(parameters, Parameters.FUNNEL, format.getMediaType().label())
            : null;
    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) return response.build();

    return prepareSuccessResponse(
            requestContext,
            queryInput.getIncludeLinkHeader() ? links : null,
            HeaderCaching.of(lastModified, etag, queryInput),
            null,
            HeaderContentDisposition.of(
                String.format("parameters.%s", format.getMediaType().fileExtension())))
        .entity(format.getEntity(parameters, apiData, requestContext))
        .build();
  }

  private Response getParameter(QueryInputParameter queryInput, ApiRequestContext requestContext) {
    ImmutableParameter.Builder builder = new ImmutableParameter.Builder();

    OgcApiDataV2 apiData = requestContext.getApi().getData();

    queryInput.getQuery().getParametersAsNodes().entrySet().stream()
        .filter(e -> e.getKey().equals(queryInput.getParameterName()))
        .map(Entry::getValue)
        .findFirst()
        .ifPresentOrElse(
            builder::schema,
            () -> {
              throw new NotFoundException();
            });

    builder.links(
        linkGenerator.generateLinks(
            requestContext.getUriCustomizer(),
            requestContext.getMediaType(),
            requestContext.getAlternateMediaTypes(),
            i18n,
            requestContext.getLanguage()));

    builder.lastModified(Optional.ofNullable(repository.getLastModified(apiData)));

    Parameter parameter = builder.build();

    ParameterFormat format =
        extensionRegistry.getExtensionsForType(ParameterFormat.class).stream()
            .filter(f -> requestContext.getMediaType().matches(f.getMediaType().type()))
            .findAny()
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported, the following media types are available: {1}",
                            requestContext.getMediaType(),
                            extensionRegistry.getExtensionsForType(ParametersFormat.class).stream()
                                .map(f -> f.getMediaType().type().toString())
                                .collect(Collectors.joining(", ")))));

    Date lastModified = getLastModified(queryInput, parameter);
    EntityTag etag =
        !format.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
                || apiData
                    .getExtension(HtmlConfiguration.class)
                    .map(HtmlConfiguration::getSendEtags)
                    .orElse(false)
            ? ETag.from(parameter, Parameter.FUNNEL, format.getMediaType().label())
            : null;
    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) return response.build();

    return prepareSuccessResponse(
            requestContext,
            queryInput.getIncludeLinkHeader() ? parameter.getLinks() : null,
            HeaderCaching.of(lastModified, etag, queryInput),
            null,
            HeaderContentDisposition.of(
                String.format("parameters.%s", format.getMediaType().fileExtension())))
        .entity(format.getEntity(parameter, apiData, requestContext))
        .build();
  }

  private Response getStoredQueries(
      QueryInputStoredQueries queryInput, ApiRequestContext requestContext) {
    ImmutableStoredQueries.Builder builder = new ImmutableStoredQueries.Builder();

    OgcApiDataV2 apiData = requestContext.getApi().getData();
    repository
        .getAll(apiData)
        .forEach(
            q -> {
              String queryId = q.getId();
              builder.addQueries(
                  new ImmutableStoredQuery.Builder()
                      .id(queryId)
                      .title(q.getTitle())
                      .description(q.getDescription())
                      .links(
                          linkGenerator.generateStoredQueryLinks(
                              requestContext.getUriCustomizer(),
                              q.getTitle().orElse(queryId),
                              queryId,
                              q.getParameterNames(),
                              apiData
                                  .getExtension(SearchConfiguration.class)
                                  .map(SearchConfiguration::isManagerEnabled)
                                  .orElse(false),
                              i18n,
                              requestContext.getLanguage()))
                      .parameters(
                          q.getParametersAsNodes().entrySet().stream()
                              .collect(
                                  Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue)))
                      .formats(
                          extensionRegistry
                              .getExtensionsForType(FeatureFormatExtension.class)
                              .stream()
                              .filter(
                                  outputFormatExtension ->
                                      outputFormatExtension.isEnabledForApi(apiData))
                              .filter(
                                  f ->
                                      Objects.nonNull(
                                          f.getFeatureContent(apiData, Optional.empty(), true)))
                              .map(
                                  f ->
                                      new AbstractMap.SimpleImmutableEntry<>(
                                          f.getMediaType().label(), f.getMediaType().parameter()))
                              .sorted(Map.Entry.comparingByKey())
                              .collect(Collectors.toUnmodifiableList()))
                      .build());
            });

    builder.links(
        linkGenerator.generateLinks(
            requestContext.getUriCustomizer(),
            requestContext.getMediaType(),
            requestContext.getAlternateMediaTypes(),
            i18n,
            requestContext.getLanguage()));

    builder.lastModified(Optional.ofNullable(repository.getLastModified(apiData)));

    StoredQueries storedQueries = builder.build();

    StoredQueriesFormat format =
        repository
            .getStoredQueriesFormatStream(apiData)
            .filter(f -> requestContext.getMediaType().matches(f.getMediaType().type()))
            .findAny()
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported, the following media types are available: {1}",
                            requestContext.getMediaType(),
                            repository
                                .getStoredQueriesFormatStream(apiData)
                                .map(f -> f.getMediaType().type().toString())
                                .collect(Collectors.joining(", ")))));

    Date lastModified = getLastModified(queryInput, storedQueries);
    EntityTag etag =
        !format.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
                || apiData
                    .getExtension(HtmlConfiguration.class)
                    .map(HtmlConfiguration::getSendEtags)
                    .orElse(false)
            ? ETag.from(storedQueries, StoredQueries.FUNNEL, format.getMediaType().label())
            : null;
    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) return response.build();

    return prepareSuccessResponse(
            requestContext,
            queryInput.getIncludeLinkHeader() ? storedQueries.getLinks() : null,
            HeaderCaching.of(lastModified, etag, queryInput),
            null,
            HeaderContentDisposition.of(
                String.format("storedQueries.%s", format.getMediaType().fileExtension())))
        .entity(format.getEntity(storedQueries, apiData, requestContext))
        .build();
  }

  private Response getQueryDefinition(
      QueryInputQueryDefinition queryInput, ApiRequestContext requestContext) {
    OgcApiDataV2 apiData = requestContext.getApi().getData();
    StoredQueryFormat format =
        repository
            .getStoredQueryFormatStream(apiData)
            .filter(f -> requestContext.getMediaType().matches(f.getMediaType().type()))
            .findAny()
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported, the following media types are available: {1}",
                            requestContext.getMediaType(),
                            repository
                                .getStoredQueryFormatStream(apiData)
                                .map(f -> f.getMediaType().type().toString())
                                .collect(Collectors.joining(", ")))));

    Date lastModified = getLastModified(queryInput);
    EntityTag etag =
        !format.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
                || apiData
                    .getExtension(HtmlConfiguration.class)
                    .map(HtmlConfiguration::getSendEtags)
                    .orElse(false)
            ? ETag.from(
                queryInput.getQuery(), QueryExpression.FUNNEL, format.getMediaType().label())
            : null;
    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) return response.build();

    return prepareSuccessResponse(
            requestContext,
            queryInput.getIncludeLinkHeader()
                ? linkGenerator.generateLinks(
                    requestContext.getUriCustomizer(),
                    requestContext.getMediaType(),
                    requestContext.getAlternateMediaTypes(),
                    i18n,
                    requestContext.getLanguage())
                : null,
            HeaderCaching.of(lastModified, etag, queryInput),
            null,
            HeaderContentDisposition.of(
                String.format(
                    "%s.%s", queryInput.getQueryId(), format.getMediaType().fileExtension())))
        .entity(format.getEntity(queryInput.getQuery(), apiData, requestContext))
        .build();
  }

  private Response executeQuery(QueryInputQuery queryInput, ApiRequestContext requestContext) {

    OgcApi api = requestContext.getApi();

    QueryExpression query =
        queryInput.getQuery().resolveParameters(requestContext.getParameters(), schemaValidator);

    FeatureFormatExtension outputFormat =
        api.getOutputFormat(
                FeatureFormatExtension.class,
                requestContext.getMediaType(),
                "/search",
                Optional.empty())
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported for this resource.",
                            requestContext.getMediaType())));

    EpsgCrs crs = query.getCrs().map(EpsgCrs::fromString).orElse(queryInput.getDefaultCrs());

    List<String> collectionIds =
        query.getCollections().size() == 1
            ? ImmutableList.of(query.getCollections().get(0))
            : query.getQueries().stream()
                .map(q -> q.getCollections().get(0))
                .collect(Collectors.toUnmodifiableList());
    QueryExpression finalQuery = query;
    List<SubQuery> queries =
        query.getCollections().size() == 1
            ? ImmutableList.of(
                getSubQuery(
                    api.getData(),
                    finalQuery.getCollections().get(0),
                    finalQuery.getFilter(),
                    ImmutableMap.of(),
                    Optional.empty(),
                    finalQuery.getSortby(),
                    finalQuery.getProperties(),
                    ImmutableList.of()))
            : query.getQueries().stream()
                .map(
                    q ->
                        getSubQuery(
                            api.getData(),
                            q.getCollections().get(0),
                            q.getFilter(),
                            finalQuery.getFilter(),
                            finalQuery.getFilterOperator(),
                            q.getSortby(),
                            q.getProperties(),
                            finalQuery.getProperties()))
                .collect(Collectors.toUnmodifiableList());

    Builder finalQueryBuilder =
        ImmutableMultiFeatureQuery.builder()
            .queries(queries)
            .maxAllowableOffset(query.getMaxAllowableOffset().orElse(0.0))
            .crs(crs)
            .limit(
                query
                    .getLimit()
                    .orElse(
                        api.getData()
                            .getExtension(FeaturesCoreConfiguration.class)
                            .map(FeaturesCoreConfiguration::getDefaultPageSize)
                            .orElseThrow()))
            .offset(query.getOffset().orElse(0));

    api.getData()
        .getExtension(FeaturesCoreConfiguration.class)
        .map(FeaturesCoreConfiguration::getCoordinatePrecision)
        .ifPresent(
            coordinatePrecision -> {
              // TODO centralize
              Integer precision;
              List<Unit<?>> units = crsInfo.getAxisUnits(crs);
              ImmutableList.Builder<Integer> precisionListBuilder = new ImmutableList.Builder<>();
              for (Unit<?> unit : units) {
                if (unit.equals(Units.METRE)) {
                  precision = coordinatePrecision.get("meter");
                  if (Objects.isNull(precision)) precision = coordinatePrecision.get("metre");
                } else if (unit.equals(Units.DEGREE)) {
                  precision = coordinatePrecision.get("degree");
                } else {
                  LOGGER.debug(
                      "Coordinate precision could not be set, unrecognised unit found: '{}'.",
                      unit.getName());
                  return;
                }
                precisionListBuilder.add(precision);
              }
              List<Integer> precisionList = precisionListBuilder.build();
              if (!precisionList.isEmpty()) {
                finalQueryBuilder.geometryPrecision(precisionList);
              }
            });

    return getResponse(
        api,
        requestContext,
        queryInput,
        query.getId(),
        query.getTitle(),
        query.getDescription(),
        finalQueryBuilder.build(),
        queryInput.getAllLinksAreLocal(),
        collectionIds,
        queryInput.getFeatureProvider(),
        outputFormat,
        queryInput.getShowsFeatureSelfLink(),
        queryInput.getIncludeLinkHeader(),
        queryInput.getDefaultCrs());
  }

  private SubQuery getSubQuery(
      OgcApiDataV2 apiData,
      String collectionId,
      Map<String, Object> filter,
      Map<String, Object> globalFilter,
      Optional<FilterOperator> filterOperator,
      List<String> sortby,
      List<String> properties,
      List<String> globalProperties) {
    {
      ensureCollectionIdExists(apiData, collectionId);

      // TODO improve
      Optional<Cql2Expression> cqlFilter = Optional.empty();
      if (!filter.isEmpty()) {
        try {
          String jsonFilter = new ObjectMapper().writeValueAsString(filter);
          cqlFilter = Optional.ofNullable(cql.read(jsonFilter, Format.JSON));
        } catch (JsonProcessingException | CqlParseException e) {
          throw new IllegalArgumentException(
              String.format("The CQL2 JSON Filter '%s' is invalid.", filter), e);
        }
      }
      if (!globalFilter.isEmpty()) {
        try {
          String jsonFilter = new ObjectMapper().writeValueAsString(globalFilter);
          if (cqlFilter.isPresent()) {
            // AND is the default
            cqlFilter =
                cqlFilter.map(
                    f ->
                        FilterOperator.OR.equals(filterOperator.orElse(FilterOperator.AND))
                            ? Or.of(f, cql.read(jsonFilter, Format.JSON))
                            : And.of(f, cql.read(jsonFilter, Format.JSON)));
          } else {
            cqlFilter = Optional.ofNullable(cql.read(jsonFilter, Format.JSON));
          }
        } catch (JsonProcessingException | CqlParseException e) {
          throw new IllegalArgumentException(
              String.format("The global CQL2 JSON Filter '%s' is invalid.", globalFilter), e);
        }
      }

      return ImmutableSubQuery.builder()
          .type(
              apiData
                  .getExtension(FeaturesCoreConfiguration.class, collectionId)
                  .flatMap(FeaturesCoreConfiguration::getFeatureType)
                  .orElse(collectionId))
          .sortKeys(
              sortby.stream()
                  .map(
                      s ->
                          s.startsWith("-")
                              ? SortKey.of(s.substring(1), Direction.DESCENDING)
                              : SortKey.of(s.replace("+", "")))
                  .collect(Collectors.toUnmodifiableList()))
          .filters(cqlFilter.stream().collect(Collectors.toUnmodifiableList()))
          .fields(
              globalProperties.isEmpty() && properties.isEmpty()
                  ? ImmutableList.of("*")
                  : globalProperties.isEmpty()
                      ? properties
                      : properties.isEmpty()
                          ? globalProperties
                          : Stream.concat(globalProperties.stream(), properties.stream())
                              .collect(Collectors.toUnmodifiableList()))
          .build();
    }
  }

  // TODO consolidate
  private Response getResponse(
      OgcApi api,
      ApiRequestContext requestContext,
      QueryInput queryInput,
      String queryId,
      Optional<String> queryTitle,
      Optional<String> queryDescription,
      MultiFeatureQuery query,
      boolean allLinksAreLocal,
      List<String> collectionIds,
      FeatureProvider2 featureProvider,
      FeatureFormatExtension outputFormat,
      boolean showsFeatureSelfLink,
      boolean includeLinkHeader,
      EpsgCrs defaultCrs) {

    ensureFeatureProviderSupportsQueries(featureProvider);

    Optional<CrsTransformer> crsTransformer = Optional.empty();

    EpsgCrs sourceCrs = null;
    EpsgCrs targetCrs = query.getCrs().orElse(defaultCrs);
    if (featureProvider.supportsCrs()) {
      sourceCrs = featureProvider.crs().getNativeCrs();
      crsTransformer = crsTransformerFactory.getTransformer(sourceCrs, targetCrs);
    }

    List<ApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

    List<Link> links =
        new StoredQueriesLinkGenerator()
            .generateFeaturesLinks(
                requestContext.getUriCustomizer(),
                query.getOffset(),
                query.getLimit(),
                requestContext.getMediaType(),
                alternateMediaTypes,
                i18n,
                requestContext.getLanguage());

    Map<String, Optional<FeatureSchema>> schemas =
        query.getQueries().stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    TypeQuery::getType,
                    q ->
                        Optional.ofNullable(
                            featureProvider.getData().getTypes().get(q.getType()))));

    Map<String, List<String>> fields =
        query.getQueries().stream()
            .collect(Collectors.toUnmodifiableMap(TypeQuery::getType, TypeQuery::getFields));

    ImmutableFeatureTransformationContextGeneric.Builder transformationContext =
        new ImmutableFeatureTransformationContextGeneric.Builder()
            .api(api)
            .apiData(api.getData())
            .featureSchemas(schemas)
            .ogcApiRequest(requestContext)
            .crsTransformer(crsTransformer)
            .codelists(
                entityRegistry.getEntitiesForType(Codelist.class).stream()
                    .collect(Collectors.toMap(PersistentEntity::getId, c -> c)))
            .defaultCrs(defaultCrs)
            .sourceCrs(Optional.ofNullable(sourceCrs))
            .links(links)
            .isFeatureCollection(true)
            .limit(query.getLimit())
            .offset(query.getOffset())
            .maxAllowableOffset(query.getMaxAllowableOffset())
            .geometryPrecision(query.getGeometryPrecision())
            .isHitsOnlyIfMore(false)
            .showsFeatureSelfLink(showsFeatureSelfLink)
            .fields(fields)
            .allLinksAreLocal(allLinksAreLocal)
            .idsIncludeCollectionId(collectionIds.size() > 1)
            .queryId(queryId)
            .queryTitle(queryTitle)
            .queryDescription(queryDescription);

    FeatureStream featureStream;
    FeatureTokenEncoder<?> encoder;
    Map<String, PropertyTransformations> propertyTransformations;

    if (outputFormat.canEncodeFeatures()) {
      featureStream = featureProvider.multiQueries().getFeatureStream(query);

      ImmutableFeatureTransformationContextGeneric transformationContextGeneric =
          transformationContext.outputStream(new OutputStreamToByteConsumer()).build();
      encoder =
          outputFormat
              .getFeatureEncoder(transformationContextGeneric, requestContext.getLanguage())
              .get();

      propertyTransformations =
          IntStream.range(0, collectionIds.size())
              .boxed()
              .collect(
                  Collectors.toUnmodifiableMap(
                      n -> getFeatureTypeId(query, n),
                      n -> {
                        String collectionId = collectionIds.get(n);
                        PropertyTransformations pt =
                            outputFormat
                                .getPropertyTransformations(
                                    Objects.requireNonNull(
                                        api.getData().getCollections().get(collectionId)))
                                .orElseThrow();
                        if (collectionIds.size() > 1) {
                          pt =
                              new IdTransform(
                                      featureProvider, getFeatureTypeId(query, n), collectionId)
                                  .mergeInto(pt);
                        }
                        return pt.withSubstitutions(
                            ImmutableMap.of(
                                "serviceUrl", transformationContextGeneric.getServiceUrl()));
                      }));
    } else {
      throw new NotAcceptableException(
          MessageFormat.format(
              "The requested media type {0} cannot be generated, because it does not support streaming.",
              requestContext.getMediaType().type()));
    }

    Date lastModified;
    EntityTag etag = null;
    StreamingOutput streamingOutput;

    streamingOutput = stream(featureStream, false, encoder, propertyTransformations);
    lastModified = getLastModified(queryInput);

    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) return response.build();

    // TODO determine numberMatched, numberReturned and optionally return them as OGC-numberMatched
    // and OGC-numberReturned headers
    // TODO For now remove the "next" links from the headers since at this point we don't know,
    // whether there will be a next page

    return prepareSuccessResponse(
            requestContext,
            includeLinkHeader
                ? links.stream()
                    .filter(link -> !"next".equalsIgnoreCase(link.getRel()))
                    .collect(ImmutableList.toImmutableList())
                : null,
            HeaderCaching.of(lastModified, etag, queryInput),
            targetCrs,
            HeaderContentDisposition.of(
                String.format("%s.%s", queryId, outputFormat.getMediaType().fileExtension())))
        .entity(streamingOutput)
        .build();
  }

  private String getFeatureTypeId(MultiFeatureQuery query, int queryIndex) {
    return query.getQueries().get(queryIndex).getType();
  }

  private StreamingOutput stream(
      FeatureStream featureTransformStream,
      boolean failIfEmpty,
      final FeatureTokenEncoder<?> encoder,
      Map<String, PropertyTransformations> propertyTransformations) {

    return outputStream -> {
      SinkTransformed<Object, byte[]> featureSink = encoder.to(Sink.outputStream(outputStream));

      Supplier<Result> stream =
          () ->
              featureTransformStream
                  .runWith(featureSink, propertyTransformations)
                  .toCompletableFuture()
                  .join();

      run(stream, failIfEmpty);
    };
  }

  private ResultReduced<byte[]> reduce(
      FeatureStream featureTransformStream,
      boolean failIfEmpty,
      final FeatureTokenEncoder<?> encoder,
      Map<String, PropertyTransformations> propertyTransformations) {

    SinkReduced<Object, byte[]> featureSink = encoder.to(Sink.reduceByteArray());

    Supplier<ResultReduced<byte[]>> stream =
        () ->
            featureTransformStream
                .runWith(featureSink, propertyTransformations)
                .toCompletableFuture()
                .join();

    return run(stream, failIfEmpty);
  }

  private <U extends ResultBase> U run(Supplier<U> stream, boolean failIfEmpty) {
    try {
      U result = stream.get();

      result.getError().ifPresent(FeatureStream::processStreamError);

      if (result.isEmpty() && failIfEmpty) {
        throw new NotFoundException("The requested feature does not exist.");
      }

      return result;

    } catch (CompletionException e) {
      if (e.getCause() instanceof WebApplicationException) {
        throw (WebApplicationException) e.getCause();
      }
      throw new IllegalStateException("Feature stream error.", e.getCause());
    }
  }
}
