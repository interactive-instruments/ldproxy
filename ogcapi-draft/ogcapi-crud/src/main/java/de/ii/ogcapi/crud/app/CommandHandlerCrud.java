/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler.QueryInputFeature;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.QueryParameterSet;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider;
import java.io.InputStream;
import java.util.Optional;
import javax.ws.rs.core.Response;
import org.immutables.value.Value;

public interface CommandHandlerCrud {

  Response postItemsResponse(QueryInputFeatureCreate queryInput, ApiRequestContext requestContext);

  Response putItemResponse(QueryInputFeatureReplace queryInput, ApiRequestContext requestContext);

  Response patchItemResponse(QueryInputFeatureReplace queryInput, ApiRequestContext requestContext);

  Response deleteItemResponse(QueryInputFeatureDelete queryInput, ApiRequestContext requestContext);

  @Value.Immutable
  interface QueryInputFeatureCreate {

    String getCollectionId();

    String getFeatureType();

    Optional<EpsgCrs> getCrs();

    EpsgCrs getDefaultCrs();

    FeatureProvider getFeatureProvider();

    InputStream getRequestBody();
  }

  interface QueryInputFeatureWithQueryParameterSet extends QueryInputFeature {

    QueryParameterSet getQueryParameterSet();
  }

  @Value.Immutable
  interface QueryInputFeatureReplace extends QueryInputFeatureWithQueryParameterSet {

    String getFeatureType();

    InputStream getRequestBody();
  }

  @Value.Immutable
  interface QueryInputFeatureDelete extends QueryInputFeatureWithQueryParameterSet {}
}
