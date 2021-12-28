/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public interface RouteRepository {
    Stream<RoutesFormatExtension> getRoutesFormatStream(OgcApiDataV2 apiData);
    Stream<RouteFormatExtension> getRouteFormatStream(OgcApiDataV2 apiData);
    Stream<RouteDefinitionFormatExtension> getRouteDefinitionFormatStream(OgcApiDataV2 apiData);
    Routes getRoutes(OgcApiDataV2 apiData, ApiRequestContext requestContext);
    boolean routeExists(OgcApiDataV2 apiData, String routeId);
    Route getRoute(OgcApiDataV2 apiData, String routeId, RouteFormatExtension format);
    RouteDefinition getRouteDefinition(OgcApiDataV2 apiData, String routeId);
    Date getLastModified(OgcApiDataV2 apiData, String routeId);
    void writeRouteAndDefinition(OgcApiDataV2 apiData, String routeId, RouteFormatExtension format, byte[] route,
                                 RouteDefinition routeDefinition, List<Link> routeDefinitionLinks) throws IOException;
    void deleteRoute(OgcApiDataV2 apiData, String routeId) throws IOException;
    ImmutableValidationResult.Builder validate(ImmutableValidationResult.Builder builder,
                                               OgcApiDataV2 apiData);
}
