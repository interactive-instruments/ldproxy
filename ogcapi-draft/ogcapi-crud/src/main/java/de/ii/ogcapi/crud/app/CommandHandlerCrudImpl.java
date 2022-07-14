/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler.Query;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableRequestContext;
import de.ii.ogcapi.foundation.domain.ImmutableRequestContext.Builder;
import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.features.domain.FeatureChange;
import de.ii.xtraplatform.features.domain.FeatureChange.Action;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureTokenSource;
import de.ii.xtraplatform.features.domain.FeatureTransactions;
import de.ii.xtraplatform.features.domain.ImmutableFeatureChange;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.features.json.domain.FeatureTokenDecoderGeoJson;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

  @Inject
  public CommandHandlerCrudImpl(FeaturesCoreQueriesHandler queriesHandler) {
    this.queriesHandler = queriesHandler;
  }

  @Override
  public Response postItemsResponse(
      FeatureTransactions featureProvider,
      ApiMediaType mediaType,
      URICustomizer uriCustomizer,
      String collectionName,
      InputStream requestBody) {

    FeatureTokenSource featureTokenSource = getFeatureSource(mediaType, requestBody);

    // TODO: collectionName != featureType
    FeatureTransactions.MutationResult result =
        featureProvider.createFeatures(collectionName, featureTokenSource);

    result.getError().ifPresent(QueriesHandler::processStreamError);

    List<String> ids = result.getIds();

    if (ids.isEmpty()) {
      throw new IllegalArgumentException("No features found in input");
    }
    URI firstFeature = null;
    try {
      firstFeature = uriCustomizer.copy().ensureLastPathSegment(ids.get(0)).build();
    } catch (URISyntaxException e) {
      // ignore
    }

    if (featureProvider instanceof FeatureProvider2) {
      handleChange(
          (FeatureProvider2) featureProvider,
          collectionName,
          ids,
          result.getSpatialExtent(),
          convertTemporalExtent(result.getTemporalExtent()),
          Action.CREATE);
    }

    return Response.created(firstFeature).build();
  }

  @Override
  public Response putItemResponse(
      QueryInputPutFeature queryInput, ApiRequestContext requestContext) {

    EntityTag eTag = getETag(queryInput, requestContext);

    Response.ResponseBuilder response =
        queriesHandler.evaluatePreconditions(requestContext, null, eTag);
    if (Objects.nonNull(response)) return response.build();

    FeatureTokenSource featureTokenSource =
        getFeatureSource(requestContext.getMediaType(), queryInput.getRequestBody());

    FeatureTransactions.MutationResult result =
        queryInput
            .getFeatureProvider()
            .transactions()
            .updateFeature(
                queryInput.getFeatureType(), queryInput.getFeatureId(), featureTokenSource);

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

  private EntityTag getETag(QueryInputPutFeature queryInput, ApiRequestContext requestContext) {
    // TODO update
    try {
      ImmutableRequestContext requestContextGeoJson =
          new Builder()
              .from(requestContext)
              .requestUri(
                  requestContext.getUriCustomizer().addParameter("schema", "receivables").build())
              .mediaType(
                  new ImmutableApiMediaType.Builder()
                      .type(new MediaType("application", "geo+json"))
                      .label("GeoJSON")
                      .parameter("json")
                      .build())
              .addAlternateMediaTypes(
                  new ImmutableApiMediaType.Builder()
                      .type(MediaType.TEXT_HTML_TYPE)
                      .label("HTML")
                      .parameter("html")
                      .build())
              .build();

      Response response = queriesHandler.handle(Query.FEATURE, queryInput, requestContextGeoJson);

      return response.getEntityTag();
    } catch (URISyntaxException e) {
      return null;
    }
  }

  @Override
  public Response deleteItemResponse(
      FeatureTransactions featureProvider, String collectionName, String featureId) {

    // TODO indicate whether a feature has been deleted or not (otherwise feature counts will become
    // incorrect)
    FeatureTransactions.MutationResult result =
        featureProvider.deleteFeature(collectionName, featureId);

    result.getError().ifPresent(QueriesHandler::processStreamError);

    if (featureProvider instanceof FeatureProvider2) {
      handleChange(
          (FeatureProvider2) featureProvider,
          collectionName,
          ImmutableList.of(featureId),
          result.getSpatialExtent(),
          convertTemporalExtent(result.getTemporalExtent()),
          Action.DELETE);
    }

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
    featureProvider.getFeatureChangeHandler().handle(change);
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
      ApiMediaType mediaType, InputStream requestBody) {

    FeatureTokenDecoderGeoJson featureTokenDecoderGeoJson = new FeatureTokenDecoderGeoJson();

    return Source.inputStream(requestBody).via(featureTokenDecoderGeoJson);
  }
}
