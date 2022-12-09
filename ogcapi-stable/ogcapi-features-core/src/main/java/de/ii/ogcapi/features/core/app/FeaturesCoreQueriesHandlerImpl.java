/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeatureLinksGenerator;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.core.domain.FeaturesLinksGenerator;
import de.ii.ogcapi.features.core.domain.ImmutableFeatureTransformationContextGeneric;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HeaderCaching;
import de.ii.ogcapi.foundation.domain.HeaderContentDisposition;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueryHandler;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureStream;
import de.ii.xtraplatform.features.domain.FeatureStream.Result;
import de.ii.xtraplatform.features.domain.FeatureStream.ResultBase;
import de.ii.xtraplatform.features.domain.FeatureStream.ResultReduced;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import de.ii.xtraplatform.streams.domain.OutputStreamToByteConsumer;
import de.ii.xtraplatform.streams.domain.Reactive.Sink;
import de.ii.xtraplatform.streams.domain.Reactive.SinkReduced;
import de.ii.xtraplatform.streams.domain.Reactive.SinkTransformed;
import de.ii.xtraplatform.strings.domain.StringTemplateFilters;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class FeaturesCoreQueriesHandlerImpl implements FeaturesCoreQueriesHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FeaturesCoreQueriesHandlerImpl.class);

  private final I18n i18n;
  private final CrsTransformerFactory crsTransformerFactory;
  private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;
  private final EntityRegistry entityRegistry;

  @Inject
  public FeaturesCoreQueriesHandlerImpl(
      I18n i18n, CrsTransformerFactory crsTransformerFactory, EntityRegistry entityRegistry) {
    this.i18n = i18n;
    this.crsTransformerFactory = crsTransformerFactory;
    this.entityRegistry = entityRegistry;

    this.queryHandlers =
        ImmutableMap.of(
            Query.FEATURES, QueryHandler.with(QueryInputFeatures.class, this::getItemsResponse),
            Query.FEATURE, QueryHandler.with(QueryInputFeature.class, this::getItemResponse));
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

  private Response getItemsResponse(
      QueryInputFeatures queryInput, ApiRequestContext requestContext) {

    OgcApi api = requestContext.getApi();
    String collectionId = queryInput.getCollectionId();
    FeatureQuery query = queryInput.getQuery();

    Optional<Integer> defaultPageSize = queryInput.getDefaultPageSize();

    FeatureFormatExtension outputFormat =
        api.getOutputFormat(
                FeatureFormatExtension.class,
                requestContext.getMediaType(),
                "/collections/" + collectionId + "/items",
                Optional.of(collectionId))
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported for this resource.",
                            requestContext.getMediaType())));

    return getResponse(
        requestContext,
        api,
        collectionId,
        null,
        queryInput,
        query,
        queryInput.getFeatureProvider(),
        getFeaturesLinks(
            requestContext, query, defaultPageSize, requestContext.getAlternateMediaTypes()),
        outputFormat,
        queryInput.getShowsFeatureSelfLink(),
        queryInput.getDefaultCrs());
  }

  private Response getItemResponse(QueryInputFeature queryInput, ApiRequestContext requestContext) {

    OgcApi api = requestContext.getApi();
    String collectionId = queryInput.getCollectionId();
    String featureId = queryInput.getFeatureId();
    FeatureQuery query = queryInput.getQuery();

    FeatureFormatExtension outputFormat =
        api.getOutputFormat(
                FeatureFormatExtension.class,
                requestContext.getMediaType(),
                "/collections/" + collectionId + "/items/" + featureId,
                Optional.of(collectionId))
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported for this resource.",
                            requestContext.getMediaType())));

    String persistentUri = null;
    Optional<String> template =
        api.getData().getCollections().get(collectionId).getPersistentUriTemplate();
    if (template.isPresent()) {
      persistentUri = StringTemplateFilters.applyTemplate(template.get(), featureId);
    }

    return getResponse(
        requestContext,
        api,
        collectionId,
        featureId,
        queryInput,
        query,
        queryInput.getFeatureProvider(),
        getFeatureLinks(
            requestContext,
            persistentUri,
            outputFormat.getCollectionMediaType(),
            requestContext.getAlternateMediaTypes()),
        outputFormat,
        false,
        queryInput.getDefaultCrs());
  }

  // Does it make sense to refactor this method?
  @SuppressWarnings({
    "PMD.CognitiveComplexity",
    "PMD.CyclomaticComplexity",
    "PMD.NPathComplexity",
    "PMD.ExcessiveMethodLength",
    "PMD.UnusedPrivateMethod" // unclear why PMD reports this warning
  })
  private Response getResponse(
      ApiRequestContext requestContext,
      OgcApi api,
      String collectionId,
      String featureId,
      QueryInput queryInput,
      FeatureQuery query,
      FeatureProvider2 featureProvider,
      List<Link> links,
      FeatureFormatExtension outputFormat,
      boolean showsFeatureSelfLink,
      EpsgCrs defaultCrs) {

    ensureCollectionIdExists(api.getData(), collectionId);
    ensureFeatureProviderSupportsQueries(featureProvider);

    Optional<CrsTransformer> crsTransformer = Optional.empty();

    EpsgCrs sourceCrs = null;
    EpsgCrs targetCrs = query.getCrs().orElse(defaultCrs);
    if (featureProvider.supportsCrs()) {
      sourceCrs = featureProvider.crs().getNativeCrs();
      crsTransformer = crsTransformerFactory.getTransformer(sourceCrs, targetCrs);
    }

    FeatureTypeConfigurationOgcApi collectionData =
        Objects.requireNonNull(api.getData().getCollections().get(collectionId));
    String featureTypeId = getFeatureType(collectionData);

    ImmutableFeatureTransformationContextGeneric.Builder transformationContext =
        new ImmutableFeatureTransformationContextGeneric.Builder()
            .api(api)
            .apiData(api.getData())
            .featureSchemas(
                ImmutableMap.of(
                    collectionId,
                    Optional.ofNullable(featureProvider.getData().getTypes().get(featureTypeId))))
            .ogcApiRequest(requestContext)
            .crsTransformer(crsTransformer)
            .codelists(
                entityRegistry.getEntitiesForType(Codelist.class).stream()
                    .collect(Collectors.toMap(PersistentEntity::getId, c -> c)))
            .defaultCrs(defaultCrs)
            .sourceCrs(Optional.ofNullable(sourceCrs))
            .links(links)
            .isFeatureCollection(Objects.isNull(featureId))
            .isHitsOnly(query.hitsOnly())
            .isPropertyOnly(query.propertyOnly())
            .fields(ImmutableMap.of(collectionId, query.getFields()))
            .limit(query.getLimit())
            .offset(query.getOffset())
            .maxAllowableOffset(query.getMaxAllowableOffset())
            .geometryPrecision(query.getGeometryPrecision())
            .showsFeatureSelfLink(showsFeatureSelfLink);

    FeatureStream featureStream;
    FeatureTokenEncoder<?> encoder;
    Map<String, PropertyTransformations> propertyTransformations = ImmutableMap.of();

    if (outputFormat.canPassThroughFeatures()
        && featureProvider.supportsPassThrough()
        && outputFormat.getMediaType().matches(featureProvider.passThrough().getMediaType())) {
      featureStream = featureProvider.passThrough().getFeatureStreamPassThrough(query);
      ImmutableFeatureTransformationContextGeneric transformationContextGeneric =
          transformationContext.outputStream(new OutputStreamToByteConsumer()).build();
      encoder =
          outputFormat
              .getFeatureEncoderPassThrough(
                  transformationContextGeneric, requestContext.getLanguage())
              .get();
    } else if (outputFormat.canEncodeFeatures()) {
      featureStream = featureProvider.queries().getFeatureStream(query);

      ImmutableFeatureTransformationContextGeneric transformationContextGeneric =
          transformationContext.outputStream(new OutputStreamToByteConsumer()).build();
      encoder =
          outputFormat
              .getFeatureEncoder(transformationContextGeneric, requestContext.getLanguage())
              .get();

      propertyTransformations =
          outputFormat
              .getPropertyTransformations(collectionData)
              .map(
                  pt ->
                      ImmutableMap.of(
                          featureTypeId,
                          pt.withSubstitutions(
                              FeaturesCoreProviders.DEFAULT_SUBSTITUTIONS.apply(
                                  transformationContextGeneric.getServiceUrl()))))
              .orElse(ImmutableMap.of());
    } else {
      throw new NotAcceptableException(
          MessageFormat.format(
              "The requested media type {0} cannot be generated, because it does not support streaming.",
              requestContext.getMediaType().type()));
    }

    Date lastModified = null;
    EntityTag etag = null;
    byte[] bytes = null;
    StreamingOutput streamingOutput = null;

    if (Objects.nonNull(featureId)
        && (!outputFormat.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
            || api.getData()
                .getExtension(HtmlConfiguration.class, collectionId)
                .map(HtmlConfiguration::getSendEtags)
                .orElse(false))) {
      ResultReduced<byte[]> result = reduce(featureStream, encoder, propertyTransformations);

      bytes = result.reduced();

      if (result.getETag().isPresent()) {
        etag = result.getETag().get();
        LOGGER.debug("ETAG {}", etag);
      }
    } else {
      streamingOutput =
          stream(featureStream, Objects.nonNull(featureId), encoder, propertyTransformations);
      lastModified = getLastModified(queryInput);
    }

    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) {
      return response.build();
    }

    // open issues:
    // determine numberMatched, numberReturned and optionally return them as OGC-numberMatched
    // and OGC-numberReturned headers;
    // for now remove the "next" links from the headers since at this point we don't know
    // whether there will be a next page;
    // see https://github.com/interactive-instruments/ldproxy/issues/812

    return prepareSuccessResponse(
            requestContext,
            queryInput.getIncludeLinkHeader()
                ? links.stream()
                    .filter(link -> !"next".equalsIgnoreCase(link.getRel()))
                    .collect(ImmutableList.toImmutableList())
                : ImmutableList.of(),
            HeaderCaching.of(lastModified, etag, queryInput),
            targetCrs,
            HeaderContentDisposition.of(
                String.format(
                    "%s.%s",
                    Objects.isNull(featureId) ? collectionId : featureId,
                    outputFormat.getMediaType().fileExtension())))
        .entity(Objects.nonNull(bytes) ? bytes : streamingOutput)
        .build();
  }

  private String getFeatureType(FeatureTypeConfigurationOgcApi collectionData) {
    return collectionData
        .getExtension(FeaturesCoreConfiguration.class)
        .flatMap(FeaturesCoreConfiguration::getFeatureType)
        .orElse(collectionData.getId());
  }

  private List<Link> getFeaturesLinks(
      ApiRequestContext requestContext,
      FeatureQuery query,
      Optional<Integer> defaultPageSize,
      List<ApiMediaType> alternateMediaTypes) {
    return new FeaturesLinksGenerator()
        .generateLinks(
            requestContext.getUriCustomizer(),
            query.getOffset(),
            query.getLimit(),
            defaultPageSize.orElse(0),
            requestContext.getMediaType(),
            alternateMediaTypes,
            i18n,
            requestContext.getLanguage());
  }

  private List<Link> getFeatureLinks(
      ApiRequestContext requestContext,
      String canonicalUri,
      ApiMediaType collectionMediaType,
      List<ApiMediaType> alternateMediaTypes) {
    return new FeatureLinksGenerator()
        .generateLinks(
            requestContext.getUriCustomizer(),
            requestContext.getMediaType(),
            alternateMediaTypes,
            collectionMediaType,
            canonicalUri,
            i18n,
            requestContext.getLanguage());
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
      final FeatureTokenEncoder<?> encoder,
      Map<String, PropertyTransformations> propertyTransformations) {

    SinkReduced<Object, byte[]> featureSink = encoder.to(Sink.reduceByteArray());

    Supplier<ResultReduced<byte[]>> stream =
        () ->
            featureTransformStream
                .runWith(featureSink, propertyTransformations)
                .toCompletableFuture()
                .join();

    return run(stream, true);
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
      throw new IllegalStateException("Feature stream error.", e);
    }
  }
}
