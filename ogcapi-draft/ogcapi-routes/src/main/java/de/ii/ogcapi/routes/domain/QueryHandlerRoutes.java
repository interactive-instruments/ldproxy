/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.domain;

import de.ii.ogcapi.foundation.domain.PermissionGroup;
import de.ii.ogcapi.foundation.domain.PermissionGroup.Base;
import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.foundation.domain.QueryIdentifier;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import java.util.Optional;
import org.immutables.value.Value;

public interface QueryHandlerRoutes extends QueriesHandler<QueryHandlerRoutes.Query> {

  String GROUP_ROUTES = "routes";
  PermissionGroup GROUP_ROUTES_READ =
      PermissionGroup.of(Base.READ, GROUP_ROUTES, "access stored routes and their definition");
  PermissionGroup GROUP_ROUTES_WRITE =
      PermissionGroup.of(
          Base.WRITE, GROUP_ROUTES, "compute and store routes, delete stored routes");

  enum Query implements QueryIdentifier {
    COMPUTE_ROUTE,
    DELETE_ROUTE,
    GET_ROUTES,
    GET_ROUTE,
    GET_ROUTE_DEFINITION
  }

  @Value.Immutable
  interface QueryInputComputeRoute extends QueryInput {
    RouteDefinition getDefinition();

    String getRouteId();

    FeatureQuery getQuery();

    FeatureProvider2 getFeatureProvider();

    String getFeatureTypeId();

    EpsgCrs getDefaultCrs();

    Optional<String> getCrs();

    String getSpeedLimitUnit();

    Optional<Double> getElevationProfileSimplificationTolerance();
  }

  @Value.Immutable
  interface QueryInputRoutes extends QueryInput {
    RouteDefinitionInfo getTemplateInfo();
  }

  @Value.Immutable
  interface QueryInputRoute extends QueryInput {
    String getRouteId();
  }
}
