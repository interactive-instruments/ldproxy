/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.routes.app.encoder.FeatureEncoderRoutes;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;

import java.util.Locale;
import java.util.Optional;

import static de.ii.ldproxy.ogcapi.routes.domain.PathParameterRouteId.ROUTE_ID_PATTERN;

public interface RouteFormatExtension extends FormatExtension {

    @Override
    default boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(RoutingConfiguration.class)
                      .filter(RoutingConfiguration::getEnabled)
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

    @Override
    default String getPathPattern() {
        return "^/routes(?:/"+ROUTE_ID_PATTERN+")?/?$";
    }

    default boolean canEncodeFeatures() {
        return false;
    }

    default Optional<FeatureTokenEncoder<?>> getFeatureEncoder(
        FeatureTransformationContextRoutes transformationContext,
        Optional<Locale> language) {
        return Optional.of(new FeatureEncoderRoutes(transformationContext));
    }

    byte[] getRouteAsByteArray(Route route, OgcApiDataV2 apiData, ApiRequestContext requestContext);

    default String getFileExtension() { return getMediaType().fileExtension(); }
}
