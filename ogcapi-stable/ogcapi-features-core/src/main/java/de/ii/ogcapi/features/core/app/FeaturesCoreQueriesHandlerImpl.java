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
import de.ii.ogcapi.features.core.domain.Profile;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.HeaderCaching;
import de.ii.ogcapi.foundation.domain.HeaderContentDisposition;
import de.ii.ogcapi.foundation.domain.HeaderItems;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.foundation.domain.QueryHandler;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureStream;
import de.ii.xtraplatform.features.domain.FeatureStream.Result;
import de.ii.xtraplatform.features.domain.FeatureStream.ResultBase;
import de.ii.xtraplatform.features.domain.FeatureStream.ResultReduced;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
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

  private Response getItemsResponse(
      QueryInputFeatures queryInput, ApiRequestContext requestContext) {

    OgcApi api = requestContext.getApi();
    String collectionId = queryInput.getCollectionId();
    FeatureQuery query = queryInput.getQuery();

    Optional<Integer> defaultPageSize = queryInput.getDefaultPageSize();
    boolean onlyHitsIfMore = false; // TODO check

    FeatureFormatExtension outputFormat =
        api.getOutputFormat(
                FeatureFormatExtension.class,
                requestContext.getMediaType(),
                Optional.of(collectionId))
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported for this resource.",
                            requestContext.getMediaType().type())));

    if (query.hitsOnly() && !outputFormat.supportsHitsOnly()) {
      throw new NotAcceptableException(
          MessageFormat.format(
              "The requested media type ''{0}'' does not support ''resultType=hits''.",
              requestContext.getMediaType().type()));
    }

    return getResponse(
        api,
        requestContext,
        collectionId,
        null,
        queryInput,
        query,
        queryInput.getProfile(),
        queryInput.getFeatureProvider(),
        null,
        outputFormat,
        onlyHitsIfMore,
        defaultPageSize,
        queryInput.getShowsFeatureSelfLink(),
        queryInput.getIncludeLinkHeader(),
        queryInput.getDefaultCrs(),
        queryInput.sendResponseAsStream());
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
                Optional.of(collectionId))
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported for this resource.",
                            requestContext.getMediaType())));

    if (outputFormat.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
        && !api.getData()
            .getExtension(HtmlConfiguration.class, collectionId)
            .map(HtmlConfiguration::getSendEtags)
            .orElse(false)) {
      query = ImmutableFeatureQuery.builder().from(query).eTag(Optional.empty()).build();
    }

    String persistentUri = null;
    Optional<String> template =
        api.getData().getCollections().get(collectionId).getPersistentUriTemplate();
    if (template.isPresent()) {
      persistentUri = StringTemplateFilters.applyTemplate(template.get(), featureId);
    }

    boolean sendResponseAsStream =
        outputFormat.getMediaType().type().equals(MediaType.TEXT_HTML_TYPE)
            && api.getData()
                .getExtension(HtmlConfiguration.class, collectionId)
                .map(HtmlConfiguration::getSendEtags)
                .orElse(false);

    return getResponse(
        api,
        requestContext,
        collectionId,
        featureId,
        queryInput,
        query,
        queryInput.getProfile(),
        queryInput.getFeatureProvider(),
        persistentUri,
        outputFormat,
        false,
        Optional.empty(),
        false,
        queryInput.getIncludeLinkHeader(),
        queryInput.getDefaultCrs(),
        sendResponseAsStream);
  }

  private Response getResponse(
      OgcApi api,
      ApiRequestContext requestContext,
      String collectionId,
      String featureId,
      QueryInput queryInput,
      FeatureQuery query,
      Optional<Profile> requestedProfile,
      FeatureProvider2 featureProvider,
      String canonicalUri,
      FeatureFormatExtension outputFormat,
      boolean onlyHitsIfMore,
      Optional<Integer> defaultPageSize,
      boolean showsFeatureSelfLink,
      boolean includeLinkHeader,
      EpsgCrs defaultCrs,
      boolean sendResponseAsStream) {

    QueriesHandler.ensureCollectionIdExists(api.getData(), collectionId);
    QueriesHandler.ensureFeatureProviderSupportsQueries(featureProvider);

    // negotiate profile, if the format does not support the selected profile
    Optional<Profile> profile = requestedProfile.map(outputFormat::negotiateProfile);

    Optional<CrsTransformer> crsTransformer = Optional.empty();

    EpsgCrs sourceCrs = null;
    EpsgCrs targetCrs = query.getCrs().orElse(defaultCrs);
    if (featureProvider.supportsCrs()) {
      sourceCrs = featureProvider.crs().getNativeCrs();
      crsTransformer = crsTransformerFactory.getTransformer(sourceCrs, targetCrs);
    }

    List<ApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

    List<Link> links =
        Objects.isNull(featureId)
            ? new FeaturesLinksGenerator()
                .generateLinks(
                    requestContext.getUriCustomizer(),
                    query.getOffset(),
                    query.getLimit(),
                    defaultPageSize.orElse(0),
                    profile,
                    requestContext.getMediaType(),
                    alternateMediaTypes,
                    i18n,
                    requestContext.getLanguage())
            : new FeatureLinksGenerator()
                .generateLinks(
                    requestContext.getUriCustomizer(),
                    profile,
                    requestContext.getMediaType(),
                    alternateMediaTypes,
                    outputFormat.getCollectionMediaType(),
                    canonicalUri,
                    i18n,
                    requestContext.getLanguage());

    String featureTypeId =
        api.getData()
            .getCollections()
            .get(collectionId)
            .getExtension(FeaturesCoreConfiguration.class)
            .map(cfg -> cfg.getFeatureType().orElse(collectionId))
            .orElse(collectionId);

    Optional<FeatureSchema> schema =
        Optional.ofNullable(featureProvider.getData().getTypes().get(featureTypeId));
    ImmutableFeatureTransformationContextGeneric.Builder transformationContext =
        new ImmutableFeatureTransformationContextGeneric.Builder()
            .api(api)
            .apiData(api.getData())
            .featureSchemas(ImmutableMap.of(collectionId, schema))
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
            .fields(ImmutableMap.of(collectionId, query.getFields()))
            .limit(query.getLimit())
            .offset(query.getOffset())
            .maxAllowableOffset(query.getMaxAllowableOffset())
            .geometryPrecision(query.getGeometryPrecision())
            .isHitsOnlyIfMore(onlyHitsIfMore)
            .showsFeatureSelfLink(showsFeatureSelfLink)
            .profile(profile);

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
              .getPropertyTransformations(
                  api.getData().getCollections().get(collectionId), schema, profile)
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

    if (sendResponseAsStream) {
      streamingOutput =
          stream(featureStream, Objects.nonNull(featureId), encoder, propertyTransformations);
      lastModified = getLastModified(queryInput);

    } else {
      ResultReduced<byte[]> result = reduce(featureStream, true, encoder, propertyTransformations);

      bytes = result.reduced();

      if (result.getETag().isPresent()) {
        etag = result.getETag().get();
        LOGGER.debug("ETAG {}", etag);
      }
    }

    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) return response.build();

    // TODO determine numberMatched, numberReturned and optionally return them as OGC-numberMatched
    // and OGC-numberReturned headers also when streaming the response
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
            outputFormat.getContentCrs(targetCrs),
            HeaderContentDisposition.of(
                String.format(
                    "%s.%s",
                    Objects.isNull(featureId) ? collectionId : featureId,
                    outputFormat.getMediaType().fileExtension())),
            Objects.isNull(featureId) && !sendResponseAsStream
                ? HeaderItems.of(
                    outputFormat.getNumberMatched(bytes), outputFormat.getNumberReturned(bytes))
                : HeaderItems.of())
        .entity(Objects.nonNull(bytes) ? bytes : streamingOutput)
        .build();
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
