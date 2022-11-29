/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler.Query;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableRequestContext.Builder;
import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureChange;
import de.ii.xtraplatform.features.domain.FeatureChange.Action;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureTokenSource;
import de.ii.xtraplatform.features.domain.FeatureTransactions;
import de.ii.xtraplatform.features.domain.ImmutableFeatureChange;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.features.json.domain.FeatureTokenDecoderGeoJson;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

@Singleton
@AutoBind
public class CommandHandlerCrudImpl implements CommandHandlerCrud {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommandHandlerCrudImpl.class);

  private final FeaturesCoreQueriesHandler queriesHandler;
  private final ExtensionRegistry extensionRegistry;
  private List<? extends FormatExtension> formats;

  @Inject
  public CommandHandlerCrudImpl(
      FeaturesCoreQueriesHandler queriesHandler, ExtensionRegistry extensionRegistry) {
    this.queriesHandler = queriesHandler;
    this.extensionRegistry = extensionRegistry;
  }

  @Override
  public Response postItemsResponse(
      QueryInputFeatureCreate queryInput, ApiRequestContext requestContext) {

    EpsgCrs crs = queryInput.getCrs().orElseGet(queryInput::getDefaultCrs);

    FeatureTokenSource featureTokenSource =
        getFeatureSource(
            requestContext.getMediaType(), queryInput.getRequestBody(), Optional.empty());

    FeatureTransactions.MutationResult result =
        queryInput
            .getFeatureProvider()
            .transactions()
            .createFeatures(queryInput.getFeatureType(), featureTokenSource, crs);

    result.getError().ifPresent(QueriesHandler::processStreamError);

    List<String> ids = result.getIds();

    if (ids.isEmpty()) {
      throw new IllegalArgumentException("No features found in input");
    }
    URI firstFeature = null;
    try {
      firstFeature =
          requestContext.getUriCustomizer().copy().ensureLastPathSegment(ids.get(0)).build();
    } catch (URISyntaxException e) {
      // ignore
    }

    handleChange(
        queryInput.getFeatureProvider(),
        queryInput.getCollectionId(),
        ids,
        result.getSpatialExtent(),
        convertTemporalExtent(result.getTemporalExtent()),
        Action.CREATE);

    return Response.created(firstFeature).build();
  }

  @Override
  public Response putItemResponse(
      QueryInputFeatureReplace queryInput, ApiRequestContext requestContext) {

    EpsgCrs crs = queryInput.getQuery().getCrs().orElseGet(queryInput::getDefaultCrs);

    Response feature = getFeatureWithETag(queryInput, requestContext, crs);
    EntityTag eTag = feature.getEntityTag();

    Response.ResponseBuilder response =
        queriesHandler.evaluatePreconditions(requestContext, null, eTag);
    if (Objects.nonNull(response)) return response.build();

    FeatureTokenSource featureTokenSource =
        getFeatureSource(
            requestContext.getMediaType(), queryInput.getRequestBody(), Optional.empty());

    FeatureTransactions.MutationResult result =
        queryInput
            .getFeatureProvider()
            .transactions()
            .updateFeature(
                queryInput.getFeatureType(),
                queryInput.getFeatureId(),
                featureTokenSource,
                crs,
                false);

    result.getError().ifPresent(QueriesHandler::processStreamError);

    handleChange(
        queryInput.getFeatureProvider(),
        queryInput.getCollectionId(),
        result.getIds(),
        result.getSpatialExtent(),
        convertTemporalExtent(result.getTemporalExtent()),
        Action.UPDATE);

    return Response.noContent().build();
  }

  @Override
  public Response patchItemResponse(
      QueryInputFeatureReplace queryInput, ApiRequestContext requestContext) {

    EpsgCrs crs = queryInput.getQuery().getCrs().orElseGet(queryInput::getDefaultCrs);

    Response feature = getFeatureWithETag(queryInput, requestContext, crs);
    EntityTag eTag = feature.getEntityTag();

    Response.ResponseBuilder response =
        queriesHandler.evaluatePreconditions(requestContext, null, eTag);
    if (Objects.nonNull(response)) return response.build();

    byte[] prev = (byte[]) feature.getEntity();
    InputStream merged =
        new SequenceInputStream(new ByteArrayInputStream(prev), queryInput.getRequestBody());
    LOGGER.debug("PREV {}", new String(prev, StandardCharsets.UTF_8));

    FeatureTokenSource mergedSource =
        getFeatureSource(
            requestContext.getMediaType(),
            merged,
            Optional.of(FeatureTransactions.PATCH_NULL_VALUE));

    FeatureTransactions.MutationResult result =
        queryInput
            .getFeatureProvider()
            .transactions()
            .updateFeature(
                queryInput.getFeatureType(), queryInput.getFeatureId(), mergedSource, crs, true);

    result.getError().ifPresent(QueriesHandler::processStreamError);

    handleChange(
        queryInput.getFeatureProvider(),
        queryInput.getCollectionId(),
        result.getIds(),
        result.getSpatialExtent(),
        convertTemporalExtent(result.getTemporalExtent()),
        Action.UPDATE);

    return Response.noContent().build();
  }

  private Response getFeatureWithETag(
      QueryInputFeatureReplace queryInput, ApiRequestContext requestContext, EpsgCrs crs) {
    try {
      if (formats == null) {
        formats = extensionRegistry.getExtensionsForType(FeatureFormatExtension.class);
      }

      ApiRequestContext requestContextGeoJson =
          new Builder()
              .from(requestContext)
              .requestUri(
                  requestContext
                      .getUriCustomizer()
                      .addParameter("schema", "receivables")
                      .addParameter("crs", crs.toUriString())
                      .build())
              .mediaType(
                  new ImmutableApiMediaType.Builder()
                      .type(new MediaType("application", "geo+json"))
                      .label("GeoJSON")
                      .parameter("json")
                      .build())
              .alternateMediaTypes(
                  formats.stream()
                      .filter(
                          f ->
                              f.isEnabledForApi(
                                  requestContext.getApi().getData(), queryInput.getCollectionId()))
                      .map(FormatExtension::getMediaType)
                      .filter(
                          mediaType -> !"geo+json".equalsIgnoreCase(mediaType.type().getSubtype()))
                      .collect(Collectors.toUnmodifiableSet()))
              .build();

      return queriesHandler.handle(Query.FEATURE, queryInput, requestContextGeoJson);
    } catch (URISyntaxException e) {
      return null;
    }
  }

  @Override
  public Response deleteItemResponse(
      QueryInputFeatureDelete queryInput, ApiRequestContext requestContext) {

    FeatureTransactions.MutationResult result =
        queryInput
            .getFeatureProvider()
            .transactions()
            .deleteFeature(queryInput.getCollectionId(), queryInput.getFeatureId());

    result.getError().ifPresent(QueriesHandler::processStreamError);

    handleChange(
        queryInput.getFeatureProvider(),
        queryInput.getCollectionId(),
        result.getIds(),
        result.getSpatialExtent(),
        convertTemporalExtent(result.getTemporalExtent()),
        Action.DELETE);

    return Response.noContent().build();
  }

  private void handleChange(
      FeatureProvider2 featureProvider,
      String collectionId,
      List<String> ids,
      Optional<BoundingBox> bbox,
      Optional<Interval> interval,
      Action action) {
    FeatureChange change =
        ImmutableFeatureChange.builder()
            .action(action)
            .featureType(collectionId)
            .featureIds(ids)
            .boundingBox(bbox)
            .interval(interval)
            .build();
    featureProvider.getChangeHandler().handle(change);
  }

  private Optional<Interval> convertTemporalExtent(Optional<Tuple<Long, Long>> interval) {
    if (interval.isEmpty()) {
      return Optional.empty();
    }

    Long begin = interval.get().first();
    Long end = interval.get().second();

    Instant beginInstant = Objects.nonNull(begin) ? Instant.ofEpochMilli(begin) : Instant.MIN;
    Instant endInstant = Objects.nonNull(end) ? Instant.ofEpochMilli(end) : Instant.MAX;

    return Optional.of(Interval.of(beginInstant, endInstant));
  }

  // TODO: to InputFormat extension matching the mediaType
  private static FeatureTokenSource getFeatureSource(
      ApiMediaType mediaType, InputStream requestBody, Optional<String> nullValue) {

    FeatureTokenDecoderGeoJson featureTokenDecoderGeoJson =
        new FeatureTokenDecoderGeoJson(nullValue);

    return Source.inputStream(requestBody).via(featureTokenDecoderGeoJson);
  }
}
