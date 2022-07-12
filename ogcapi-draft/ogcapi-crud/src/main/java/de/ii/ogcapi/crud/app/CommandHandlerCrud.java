/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler.QueryInputFeature;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.xtraplatform.features.domain.FeatureTransactions;
import java.io.InputStream;
import javax.ws.rs.core.Response;
import org.immutables.value.Value;

public interface CommandHandlerCrud {

  Response postItemsResponse(
      FeatureTransactions featureProvider,
      ApiMediaType mediaType,
      URICustomizer uriCustomizer,
      String collectionName,
      InputStream requestBody);

  Response putItemResponse(QueryInputPutFeature queryInput, ApiRequestContext requestContext);

  Response deleteItemResponse(
      FeatureTransactions featureProvider, String collectionName, String featureId);

  @Value.Immutable
  public interface QueryInputPutFeature extends QueryInputFeature {

    String getFeatureType();

    InputStream getRequestBody();
  }
}
