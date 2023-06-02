/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler.Query;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler.QueryInputFeature;
import de.ii.ogcapi.features.core.domain.ProfileTransformations;
import de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableRequestContext.Builder;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureChange;
import de.ii.xtraplatform.features.domain.FeatureChange.Action;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureStream;
import de.ii.xtraplatform.features.domain.FeatureTokenSource;
import de.ii.xtraplatform.features.domain.FeatureTransactions;
import de.ii.xtraplatform.features.domain.ImmutableFeatureChange;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
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
import javax.validation.constraints.NotNull;
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
  private final FeaturesCoreProviders providers;
  private List<? extends FormatExtension> formats;

  @Inject
  public CommandHandlerCrudImpl(
      FeaturesCoreQueriesHandler queriesHandler,
      ExtensionRegistry extensionRegistry,
      FeaturesCoreProviders providers) {
    this.queriesHandler = queriesHandler;
    this.extensionRegistry = extensionRegistry;
    this.providers = providers;
  }

  @Override
  public Response postItemsResponse(
      QueryInputFeatureCreate queryInput, ApiRequestContext requestContext) {

    EpsgCrs crs = queryInput.getCrs().orElseGet(queryInput::getDefaultCrs);

    FeatureTokenSource featureTokenSource =
        GeoJsonHelper.getFeatureSource(
            requestContext.getMediaType(), queryInput.getRequestBody(), Optional.empty());

    FeatureTransactions.MutationResult result =
        queryInput
            .getFeatureProvider()
            .transactions()
            .createFeatures(queryInput.getFeatureType(), featureTokenSource, crs);

    result.getError().ifPresent(FeatureStream::processStreamError);

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
        Optional.empty(),
        result.getSpatialExtent(),
        Optional.empty(),
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
    JsonNode content = GeoJsonHelper.parseFeatureResponse(feature);

    Response.ResponseBuilder response =
        queriesHandler.evaluatePreconditions(requestContext, null, eTag);
    if (Objects.nonNull(response)) return response.build();

    FeatureTokenSource featureTokenSource =
        GeoJsonHelper.getFeatureSource(
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

    result.getError().ifPresent(FeatureStream::processStreamError);

    handleChange(
        queryInput.getFeatureProvider(),
        queryInput.getCollectionId(),
        result.getIds(),
        GeoJsonHelper.getSpatialExtent(content, crs),
        result.getSpatialExtent(),
        getTemporalExtent(content, requestContext.getApi().getData(), queryInput.getCollectionId()),
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
    JsonNode content = GeoJsonHelper.parseFeatureResponse(feature);

    Response.ResponseBuilder response =
        queriesHandler.evaluatePreconditions(requestContext, null, eTag);
    if (Objects.nonNull(response)) return response.build();

    byte[] prev = (byte[]) feature.getEntity();
    InputStream merged =
        new SequenceInputStream(new ByteArrayInputStream(prev), queryInput.getRequestBody());
    LOGGER.debug("PREV {}", new String(prev, StandardCharsets.UTF_8));

    FeatureTokenSource mergedSource =
        GeoJsonHelper.getFeatureSource(
            requestContext.getMediaType(),
            merged,
            Optional.of(FeatureTransactions.PATCH_NULL_VALUE));

    FeatureTransactions.MutationResult result =
        queryInput
            .getFeatureProvider()
            .transactions()
            .updateFeature(
                queryInput.getFeatureType(), queryInput.getFeatureId(), mergedSource, crs, true);

    result.getError().ifPresent(FeatureStream::processStreamError);

    handleChange(
        queryInput.getFeatureProvider(),
        queryInput.getCollectionId(),
        result.getIds(),
        GeoJsonHelper.getSpatialExtent(content, crs),
        result.getSpatialExtent(),
        getTemporalExtent(content, requestContext.getApi().getData(), queryInput.getCollectionId()),
        convertTemporalExtent(result.getTemporalExtent()),
        Action.UPDATE);

    return Response.noContent().build();
  }

  @Override
  public Response deleteItemResponse(
      QueryInputFeature queryInput, ApiRequestContext requestContext) {

    Response feature = getFeatureWithETag(queryInput, requestContext, OgcCrs.CRS84);
    EntityTag eTag = feature.getEntityTag();
    JsonNode content = GeoJsonHelper.parseFeatureResponse(feature);

    Response.ResponseBuilder response =
        queriesHandler.evaluatePreconditions(requestContext, null, eTag);
    if (Objects.nonNull(response)) {
      return response.build();
    }

    FeatureTransactions.MutationResult result =
        queryInput
            .getFeatureProvider()
            .transactions()
            .deleteFeature(queryInput.getCollectionId(), queryInput.getFeatureId());

    result.getError().ifPresent(FeatureStream::processStreamError);

    handleChange(
        queryInput.getFeatureProvider(),
        queryInput.getCollectionId(),
        result.getIds(),
        GeoJsonHelper.getSpatialExtent(content, queryInput.getDefaultCrs()),
        result.getSpatialExtent(),
        getTemporalExtent(content, requestContext.getApi().getData(), queryInput.getCollectionId()),
        convertTemporalExtent(result.getTemporalExtent()),
        Action.DELETE);

    return Response.noContent().build();
  }

  private @NotNull Response getFeatureWithETag(
      QueryInputFeature queryInput, ApiRequestContext requestContext, EpsgCrs crs) {
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
      throw new IllegalStateException(
          String.format(
              "Could not retrieve current GeoJSON feature for evaluating preconditions. Reason: %s",
              e.getMessage()),
          e);
    }
  }

  private void handleChange(
      FeatureProvider2 featureProvider,
      String collectionId,
      List<String> ids,
      Optional<BoundingBox> oldBbox,
      Optional<BoundingBox> newBbox,
      Optional<Interval> oldInterval,
      Optional<Interval> newInterval,
      Action action) {
    FeatureChange change =
        ImmutableFeatureChange.builder()
            .action(action)
            .featureType(collectionId)
            .featureIds(ids)
            .oldBoundingBox(oldBbox)
            .newBoundingBox(newBbox)
            .oldInterval(oldInterval)
            .newInterval(newInterval)
            .build();
    featureProvider.getChangeHandler().handle(change);
  }

  private Optional<Interval> getTemporalExtent(
      JsonNode feature, OgcApiDataV2 apiData, String collectionId) {
    Optional<String> flattened =
        apiData
            .getExtension(GeoJsonConfiguration.class, collectionId)
            .map(PropertyTransformations::getTransformations)
            .map(t -> t.get(ProfileTransformations.WILDCARD))
            .flatMap(
                t ->
                    t.stream()
                        .map(PropertyTransformation::getFlatten)
                        .flatMap(Optional::stream)
                        .findFirst());
    return GeoJsonHelper.getTemporalExtent(
        feature,
        apiData
            .getCollectionData(collectionId)
            .flatMap(cd -> providers.getFeatureSchema(apiData, cd)),
        flattened);
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
}
