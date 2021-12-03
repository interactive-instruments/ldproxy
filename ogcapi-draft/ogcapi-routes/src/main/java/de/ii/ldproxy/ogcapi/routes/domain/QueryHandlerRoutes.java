/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import de.ii.ldproxy.ogcapi.domain.QueriesHandler;
import de.ii.ldproxy.ogcapi.domain.QueryIdentifier;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import org.immutables.value.Value;

import javax.ws.rs.core.MediaType;
import java.util.Optional;

public interface QueryHandlerRoutes extends QueriesHandler<QueryHandlerRoutes.Query> {

    enum Query implements QueryIdentifier {COMPUTE_ROUTE, ROUTE_DEFINITION_FORM}

    @Value.Immutable
    interface QueryInputComputeRoute extends QueryInput {
        MediaType getContentType();
        byte[] getRequestBody();
        FeatureQuery getQuery();
        FeatureProvider2 getFeatureProvider();
        String getFeatureTypeId();
        EpsgCrs getDefaultCrs();
        Optional<String> getCrs();
        String getSpeedLimitUnit();
    }

    @Value.Immutable
    interface QueryInputRouteDefinitionForm extends QueryInput {
        RouteDefinitionInfo getTemplateInfo();
    }
}
