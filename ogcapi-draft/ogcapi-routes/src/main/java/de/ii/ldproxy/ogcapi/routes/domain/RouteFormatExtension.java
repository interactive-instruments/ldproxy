/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.routes.app.FeatureEncoderRoutes;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;

import java.util.Locale;
import java.util.Optional;

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
        return "^/routes/?$";
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
}
