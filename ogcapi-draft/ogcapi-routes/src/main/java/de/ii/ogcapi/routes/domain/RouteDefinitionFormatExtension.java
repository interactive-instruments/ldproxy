/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.domain;

import static de.ii.ogcapi.routes.domain.PathParameterRouteId.ROUTE_ID_PATTERN;

import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;

public interface RouteDefinitionFormatExtension extends FormatExtension {

  @Override
  default String getPathPattern() {
    return "^/routes/" + ROUTE_ID_PATTERN + "/definition/?$";
  }

  @Override
  default boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
        .getExtension(RoutingConfiguration.class)
        .filter(RoutingConfiguration::isEnabled)
        .filter(RoutingConfiguration::isManageRoutesEnabled)
        .isPresent();
  }

  @Override
  default boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return false;
  }

  @Override
  default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return RoutingConfiguration.class;
  }

  byte[] getRouteDefinitionAsByteArray(
      RouteDefinition routeDefinition, OgcApiDataV2 apiData, ApiRequestContext requestContext);

  default String getFileExtension() {
    return getMediaType().fileExtension();
  }
}
