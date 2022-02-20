/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.domain;

import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class FeatureTransformationContextRoutes implements FeatureTransformationContext {

    public abstract RouteFormatExtension getFormat();
    public abstract Optional<String> getName();
    public abstract EpsgCrs getCrs();
    public abstract long getStartTimeNano();
    public abstract String getSpeedLimitUnit();
    public abstract Optional<Double> getElevationProfileSimplificationTolerance();
}
