/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import de.ii.ogcapi.foundation.domain.ApiMediaType;
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
import javax.ws.rs.core.Response;

public class CommandHandlerCrud {

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

  public Response putItemResponse(
      FeatureTransactions featureProvider,
      ApiMediaType mediaType,
      String collectionName,
      String featureId,
      InputStream requestBody) {

    FeatureTokenSource featureTokenSource = getFeatureSource(mediaType, requestBody);

    // TODO: collectionName != featureType
    FeatureTransactions.MutationResult result =
        featureProvider.updateFeature(collectionName, featureId, featureTokenSource);

    result.getError().ifPresent(QueriesHandler::processStreamError);

    return Response.noContent().build();
  }

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
