/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler.Query;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableRequestContext;
import de.ii.ogcapi.foundation.domain.ImmutableRequestContext.Builder;
import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.xtraplatform.features.domain.FeatureTokenSource;
import de.ii.xtraplatform.features.domain.FeatureTransactions;
import de.ii.xtraplatform.features.json.domain.FeatureTokenDecoderGeoJson;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    return Response.noContent().build();
  }

  private EntityTag getETag(QueryInputPutFeature queryInput, ApiRequestContext requestContext) {
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

    FeatureTransactions.MutationResult result =
        featureProvider.deleteFeature(collectionName, featureId);

    result.getError().ifPresent(QueriesHandler::processStreamError);

    return Response.noContent().build();
  }

  // TODO: to InputFormat extension matching the mediaType
  private static FeatureTokenSource getFeatureSource(
      ApiMediaType mediaType, InputStream requestBody) {

    FeatureTokenDecoderGeoJson featureTokenDecoderGeoJson = new FeatureTokenDecoderGeoJson();

    return Source.inputStream(requestBody).via(featureTokenDecoderGeoJson);
  }
}
