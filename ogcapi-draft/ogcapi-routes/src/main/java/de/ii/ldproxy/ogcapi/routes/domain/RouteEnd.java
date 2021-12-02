/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.features.core.domain.Geometry;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableRouteEnd.Builder.class)
public abstract class RouteEnd extends RouteComponent {

    public static String FEATURE_TYPE = "end";

    public abstract Geometry.Point getGeometry();

    public abstract Map<String, Object> getProperties();
}
